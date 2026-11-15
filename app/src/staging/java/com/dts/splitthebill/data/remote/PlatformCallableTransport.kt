package com.dts.splitthebill.data.remote

import com.dts.splitthebill.domain.repository.BackendException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import javax.inject.Inject
import javax.inject.Singleton

/** Real Firebase callable transport used by the installable staging build. */
@Singleton
class PlatformCallableTransport @Inject constructor(
    private val functions: FirebaseFunctions,
) : CallableTransport {
    override suspend fun call(name: String, request: Map<String, Any?>): Map<*, *> {
        try {
            return functions.getHttpsCallable(name).call(request).awaitResult().data
                as? Map<*, *> ?: error("Malformed callable response")
        } catch (error: FirebaseFunctionsException) {
            val details = (error.details as? Map<*, *>)
                ?.entries?.associate { it.key.toString() to it.value }
                .orEmpty()
            throw BackendException(
                errorCode = details["errorCode"] as? String ?: error.code.name,
                message = error.message ?: "The server rejected this change.",
                details = details,
                cause = error,
            )
        }
    }
}
