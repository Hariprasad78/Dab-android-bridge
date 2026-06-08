package com.harry.dabagent.security

class PackageAllowlist(private val packages: Set<String>) {
    fun isAllowed(packageName: String): Boolean = packages.isEmpty() || packages.contains(packageName)

    companion object {
        private val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

        fun isValidPackageName(value: String): Boolean = value.length in 3..223 && PACKAGE_REGEX.matches(value)

        fun parse(csv: String): PackageAllowlist = PackageAllowlist(
            csv.split(',', '\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet(),
        )
    }
}
