package com.dts.splitthebill.data.remote

/** Transport boundary for the platform-neutral callable API contract. */
interface CallableTransport {
    suspend fun call(name: String, request: Map<String, Any?>): Map<*, *>
}
