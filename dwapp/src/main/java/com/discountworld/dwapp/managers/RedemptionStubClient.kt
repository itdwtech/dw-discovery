package com.discountworld.dwapp.managers

import com.discountworld.discount.RedemptionServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.MetadataUtils
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object RedemptionStubClient {
    // Raw API key (converted to Base64 dynamically for x-api-key header)
    private var apiKey: String = "a55284c6-f470-4e07-a6d3-8d7e0b5b3c13"
    private const val SERVER_URL = "stg-api.appbiance.com"
    private const val SERVER_PORT = 443

    private var accessToken: String? = null

    fun setToken(token: String) {
        accessToken = token
    }

    fun setApiKey(key: String) {
        if (key.isNotBlank()) {
            apiKey = key
        }
    }

    private val channel: ManagedChannel by lazy {
        OkHttpChannelBuilder
            .forAddress(SERVER_URL, SERVER_PORT)
            .useTransportSecurity()
            .build()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getFormattedBase64ApiKey(key: String): String {
        return try {
            val decoded = Base64.decode(key)
            val reEncoded = Base64.encode(decoded)
            if (reEncoded == key) key else Base64.encode(key.toByteArray(Charsets.UTF_8))
        } catch (_: Exception) {
            Base64.encode(key.toByteArray(Charsets.UTF_8))
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    val stub: RedemptionServiceGrpcKt.RedemptionServiceCoroutineStub
        get() {
            val headers = Metadata()
            val apiKeyKey = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)
            val encodedApiKey = getFormattedBase64ApiKey(apiKey)
            headers.put(apiKeyKey, encodedApiKey)

            accessToken?.let {
                val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
                headers.put(authKey, "Bearer $it")
            }

            return MetadataUtils.attachHeaders(
                RedemptionServiceGrpcKt.RedemptionServiceCoroutineStub(channel),
                headers
            )
        }

    fun shutdown() {
        channel.shutdown()
    }

    suspend fun <T> grpcCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: StatusException) {
            android.util.Log.e("gRPC", "StatusException: ${e.status}", e)
            Result.failure(e)
        } catch (e: StatusRuntimeException) {
            android.util.Log.e("gRPC", "StatusRuntimeException: ${e.status}", e)
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("gRPC", "General Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
