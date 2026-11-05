package com.discountworld.dwapp.managers

import com.discountworld.discount.RedemptionServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.MetadataUtils
import kotlin.io.encoding.ExperimentalEncodingApi

object RedemptionStubClient {
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
    private fun isBase64Encoded(str: String): Boolean {
        return try {
            val decoded = kotlin.io.encoding.Base64.decode(str)
            val reEncoded = kotlin.io.encoding.Base64.encode(decoded)
            reEncoded == str
        } catch (_: Exception) {
            false
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    val stub: RedemptionServiceGrpcKt.RedemptionServiceCoroutineStub
        get() {
            val headers = Metadata()
            val apiKeyKey = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)
            val finalApiKey = if (isBase64Encoded(apiKey)) {
                apiKey
            } else {
                kotlin.io.encoding.Base64.encode(apiKey.toByteArray())
            }
            headers.put(apiKeyKey, finalApiKey)

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
