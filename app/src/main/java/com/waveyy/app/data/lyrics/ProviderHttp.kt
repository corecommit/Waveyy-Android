package com.waveyy.app.data.lyrics

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

/** Cancellable OkHttp await: coroutine cancellation cancels the call. */
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { runCatching { cancel() } }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCompleted) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (!cont.isCompleted) cont.resume(response) { _, _, _ -> response.close() }
        }
    })
}

/** URL-encodes a query/path value as UTF-8. */
fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

/** Executes [request] and returns the body string for 2xx, else null. Never throws. */
suspend fun fetchBody(
    client: okhttp3.OkHttpClient,
    request: Request
): String? {
    return runCatching {
        client.newCall(request).await().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string()?.takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}

/** Shared provider plumbing (client + user-agent request builder). */
@Singleton
open class ProviderHttp @Inject constructor(
    val client: okhttp3.OkHttpClient
) {
    /** GET with a browser user-agent (some lyric hosts reject default Java UAs). */
    fun get(url: String, referer: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Waveyy/1.0")
        if (referer != null) builder.header("Referer", referer)
        return builder.build()
    }
}
