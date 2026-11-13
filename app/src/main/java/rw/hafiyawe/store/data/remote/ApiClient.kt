package rw.hafiyawe.store.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerializationException
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.HttpException
import retrofit2.Retrofit
import rw.hafiyawe.store.core.SessionStore
import rw.hafiyawe.store.data.local.AppJson
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A failure the UI can show verbatim. `status == 0` means "the server was unreachable". */
class ApiException(val status: Int, override val message: String) : Exception(message)

/**
 * Retrofit is built once against a placeholder origin; this interceptor swaps in the
 * address the person configured in Settings, so changing servers needs no rebuild.
 */
private class BaseUrlInterceptor(private val session: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val configured = session.baseUrl.toHttpUrlOrNull() ?: return chain.proceed(request)
        val url = request.url.newBuilder()
            .scheme(configured.scheme)
            .host(configured.host)
            .port(configured.port)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}

private class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            // A native client never sends an Origin header; the API rejects foreign origins.
            .removeHeader("Origin")
            .header("User-Agent", "HAFIYAWE-Android/1.0")
            .build()
        return chain.proceed(request)
    }
}

fun buildHttpClient(cookies: CookieJar, session: SessionStore): OkHttpClient =
    OkHttpClient.Builder()
        .cookieJar(cookies)
        .addInterceptor(BaseUrlInterceptor(session))
        .addInterceptor(HeaderInterceptor())
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

fun buildApi(client: OkHttpClient): ApiService {
    val contentType = "application/json".toMediaType()
    return Retrofit.Builder()
        .baseUrl(SessionStore.DEFAULT_BASE + "/")
        .client(client)
        .addConverterFactory(AppJson.asConverterFactory(contentType))
        .build()
        .create(ApiService::class.java)
}

/** Runs a call and normalises every failure into an [ApiException] with user-facing text. */
suspend fun <T> apiCall(block: suspend () -> T): T {
    try {
        return block()
    } catch (e: ApiException) {
        throw e
    } catch (e: HttpException) {
        throw ApiException(e.code(), errorMessage(e))
    } catch (e: SerializationException) {
        throw ApiException(-1, "The server sent a response this app could not read. Please try again.")
    } catch (e: IOException) {
        throw ApiException(0, "Cannot reach the server. Check your connection and the server address in Settings.")
    }
}

private fun errorMessage(error: HttpException): String {
    val raw = runCatching { error.response()?.errorBody()?.string() }.getOrNull().orEmpty()
    val fromServer = runCatching {
        AppJson.parseToJsonElement(raw).let { element ->
            element.asObjectOrNull()?.get("error")?.toString()?.trim('"')
        }
    }.getOrNull()
    if (!fromServer.isNullOrBlank()) return fromServer
    return when (error.code()) {
        401 -> "Please sign in to continue."
        403 -> "This action is not allowed for your account."
        404 -> "That item is no longer available."
        409 -> "That was already done."
        413 -> "The file is too large."
        429 -> "Too many attempts. Please wait a moment and try again."
        503 -> "This feature is not configured on the server yet."
        else -> "Something went wrong (${error.code()}). Please try again."
    }
}

private fun kotlinx.serialization.json.JsonElement.asObjectOrNull():
    kotlinx.serialization.json.JsonObject? =
    this as? kotlinx.serialization.json.JsonObject
