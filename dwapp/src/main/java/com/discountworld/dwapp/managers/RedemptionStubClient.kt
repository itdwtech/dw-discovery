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
    private const val API_KEY = "ODZiMjNiOTEtNTZjMS00MDZlLWE1MGMtZWM1NjllNjZiNTdj"
    private const val SERVER_URL = "192.168.0.104"
    private const val SERVER_PORT = 9090

    private var accessToken: String? = null

    fun setToken(token: String) {
        accessToken = token
    }

    private val channel: ManagedChannel by lazy {
        OkHttpChannelBuilder
            .forAddress(SERVER_URL, SERVER_PORT)
            .usePlaintext()
            .build()
    }

    @OptIn(ExperimentalEncodingApi::class)
    val stub: RedemptionServiceGrpcKt.RedemptionServiceCoroutineStub
        get() {
            val headers = Metadata()
            val apiKeyKey = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)
            headers.put(apiKeyKey, API_KEY)

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
