package com.dts.splitthebill.data.remote

import com.dts.splitthebill.BuildConfig
import com.dts.splitthebill.domain.repository.BackendException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Emulator-only callable transport.
 *
 * The Android Firebase Functions SDK always requests a Firebase Installations/Instance ID
 * token, even when the Functions emulator does not require one. A credential-free demo
 * project cannot obtain that production token, so debug builds speak the documented callable
 * JSON protocol directly and send only the Auth emulator ID token.
 */
@Singleton
class PlatformCallableTransport @Inject constructor(
    private val firebaseApp: FirebaseApp,
    private val auth: FirebaseAuth,
) : CallableTransport {
    override suspend fun call(name: String, request: Map<String, Any?>): Map<*, *> =
        withContext(Dispatchers.IO) {
            val idToken = auth.currentUser?.getIdToken(false)?.awaitResult()?.token
                ?: throw BackendException("UNAUTHENTICATED", "Sign in is required.")
            val projectId = firebaseApp.options.projectId
                ?: throw IllegalStateException("Firebase project ID is missing.")
            val endpoint = URL(
                "http://${BuildConfig.FIREBASE_EMULATOR_HOST}:5001/" +
                    "$projectId/europe-west1/$name",
            )
            val connection = endpoint.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = TIMEOUT_MILLIS
                connection.readTimeout = TIMEOUT_MILLIS
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $idToken")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(JSONObject().put("data", request.toJsonValue()).toString())
                }

                val statusCode = connection.responseCode
                val responseBody = (if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                })?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val response = runCatching { JSONObject(responseBody) }.getOrElse {
                    throw BackendException(
                        errorCode = "UNAVAILABLE",
                        message = invalidResponseMessage(statusCode, responseBody),
                        cause = it,
                    )
                }
                response.optJSONObject("result")?.let(JSONObject::toMap)
                    ?: throw response.toBackendException()
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000

        fun invalidResponseMessage(statusCode: Int, responseBody: String): String =
            if (statusCode == HttpURLConnection.HTTP_NOT_FOUND &&
                responseBody.contains("does not exist", ignoreCase = true)
            ) {
                "The local Functions backend did not load. Restart the Emulator Suite " +
                    "with Node.js 22.13 or newer, then retry."
            } else {
                "The local Functions emulator returned an invalid response (HTTP $statusCode)."
            }
    }
}

private fun Map<*, *>.toJsonValue(): JSONObject = JSONObject().also { result ->
    forEach { (key, value) -> result.put(key.toString(), value.toJsonValue()) }
}

private fun Any?.toJsonValue(): Any = when (this) {
    null -> JSONObject.NULL
    is Map<*, *> -> toJsonValue()
    is Iterable<*> -> JSONArray().also { array -> forEach { array.put(it.toJsonValue()) } }
    else -> this
}

private fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { key ->
    get(key).toKotlinValue()
}

private fun JSONArray.toList(): List<Any?> = List(length()) { index -> get(index).toKotlinValue() }

private fun Any.toKotlinValue(): Any? = when (this) {
    JSONObject.NULL -> null
    is JSONObject -> toMap()
    is JSONArray -> toList()
    else -> this
}

private fun JSONObject.toBackendException(): BackendException {
    val error = optJSONObject("error")
    val details = error?.optJSONObject("details")?.toMap().orEmpty()
    return BackendException(
        errorCode = details["errorCode"] as? String
            ?: error?.optString("status")?.takeIf(String::isNotBlank)
            ?: "INTERNAL",
        message = error?.optString("message")?.takeIf(String::isNotBlank)
            ?: "The local Functions emulator rejected this change.",
        details = details,
    )
}
