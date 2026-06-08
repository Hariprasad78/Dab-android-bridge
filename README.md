# DabDeviceAgent

DabDeviceAgent is a Kotlin Android app (`com.harry.dabagent`) that acts as a DAB-over-MQTT device automation agent for authorized lab devices. It runs a foreground service, subscribes to DAB request JSON, executes supported operations through a replaceable executor layer, and publishes structured responses and status messages.

## Security warning

This project is for authorized lab devices only. It is not malware, does not silently exploit permissions, and treats privileged operations as explicit executor capabilities. Normal APK builds default to `NO_PRIVILEGE`; unsupported privileged requests return clean errors. Configure MQTT authentication, allowed packages, and allowed methods before use. The service tracks processed request IDs and returns a clean duplicate-request error for replayed IDs while the process is running.

## Architecture

- `mqtt/`: MQTT connection, topic routing, and publishing.
- `protocol/`: DAB request/response envelopes and JSON codec.
- `executor/`: `DeviceExecutor`, `NoPrivilegeExecutor`, and `SelfAdbExecutor` placeholder.
- `commands/`: method handlers and method router.
- `security/`: command policy, package allowlist, duplicate request tracking, and request validation.
- `service/`: foreground `DabAgentService` and `BootReceiver`.

`SelfAdbExecutor` constructs commands with argument lists, validates package names, key events, and APK paths, and includes TODO comments for future Shizuku/root/system-app transports.

## MQTT topics

Official DAB operation topics route by MQTT topic:

- `dab/{deviceId}/operations/list`
- `dab/{deviceId}/input/key/list`
- `dab/{deviceId}/input/key-press`
- `dab/{deviceId}/input/long-key-press`
- `dab/{deviceId}/applications/list`
- `dab/{deviceId}/applications/launch`
- `dab/{deviceId}/applications/get-state`
- `dab/{deviceId}/applications/exit`
- `dab/{deviceId}/device/info`
- `dab/{deviceId}/output/image`

Bridge-compatible topics route by the request envelope `method` field:

- Request: `dab/bridge/{bridgeId}/device/{deviceId}/request`
- Response: `dab/bridge/{bridgeId}/device/{deviceId}/response`
- Status: `dab/bridge/{bridgeId}/device/{deviceId}/status`

## Request example

```json
{
  "requestId": "req-001",
  "method": "applications/launch",
  "params": { "packageName": "com.google.android.youtube.tv" },
  "timeoutMs": 10000
}
```

## Response example

```json
{
  "requestId": "req-001",
  "method": "applications/launch",
  "status": 200,
  "success": true,
  "result": {},
  "error": null,
  "timestamp": "2026-06-08T00:00:00.000Z"
}
```

## Supported methods

- `operations/list`
- `device/info`
- `applications/list`
- `applications/launch`
- `applications/get-state`
- `applications/exit`
- `input/key/list`
- `input/key-press`
- `input/long-key-press`
- `output/image`

Bridge-compatible JSON requests also retain internal handlers for privileged maintenance operations such as `applications/clear-data`, `applications/install`, and `applications/uninstall` when explicitly allowed by policy and supported by the selected executor.


## Manual MQTT smoke tests

Subscribe to official DAB topics:

```bash
mosquitto_sub -h 127.0.0.1 -t "dab/adt4-ack/#" -v
```

Subscribe to bridge-compatible lab topics:

```bash
mosquitto_sub -h 127.0.0.1 -t "dab/bridge/harry/device/adt4-ack/#" -v
```

Publish official DAB input requests:

```bash
mosquitto_pub -h 127.0.0.1 -t "dab/adt4-ack/input/key/list" -m '{}'
mosquitto_pub -h 127.0.0.1 -t "dab/adt4-ack/input/key-press" -m '{"keyCode":"KEY_HOME"}'
mosquitto_pub -h 127.0.0.1 -t "dab/adt4-ack/input/key-press" -m '{"keyCode":"KEY_LEFT"}'
mosquitto_pub -h 127.0.0.1 -t "dab/adt4-ack/input/key-press" -m '{"keyCode":"KEY_ENTER"}'
```

Publish a bridge-compatible request that routes by JSON `method`:

```bash
mosquitto_pub -h 127.0.0.1 -t "dab/bridge/harry/device/adt4-ack/request" -m '{"requestId":"req-001","method":"input/key-press","params":{"keyCode":"KEY_HOME"},"timeoutMs":2000}'
```

Official DAB requests route by MQTT topic and responses are published to the same operation topic with `/response` appended, for example `dab/adt4-ack/input/key-press/response`. Bridge-compatible requests publish to `dab/bridge/{bridgeId}/device/{deviceId}/response`.

## Build

```bash
gradle clean assembleDebug
```

Run unit tests:

```bash
gradle testDebugUnitTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. This repository intentionally does not commit binary build artifacts such as APKs, AABs, ZIPs, or JARs; GitHub Actions uploads the APK as a workflow artifact instead.

## GitHub Actions artifact download

The workflow `.github/workflows/android-build.yml` runs on pushes and pull requests to `main`, manual dispatch, and tags beginning with `v`. Open the workflow run in GitHub Actions and download the artifact named `DabDeviceAgent-debug-<run_number>`. Test reports are uploaded as `DabDeviceAgent-test-reports-<run_number>` even when tests fail.

Tagged `v*` builds currently upload an unsigned debug APK. Signed release APK/AAB generation can be added later with Gradle signing configuration and GitHub secrets.
