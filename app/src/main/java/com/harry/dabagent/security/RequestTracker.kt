package com.harry.dabagent.security

class RequestTracker(private val maxTrackedIds: Int = 500) {
    private val seenIds = ArrayDeque<String>()
    private val seenSet = mutableSetOf<String>()

    @Synchronized
    fun markSeen(requestId: String): Boolean {
        if (requestId in seenSet) return false
        seenIds.addLast(requestId)
        seenSet.add(requestId)
        while (seenIds.size > maxTrackedIds) {
            seenSet.remove(seenIds.removeFirst())
        }
        return true
    }
}
