package com.example.easypaisasdk.managers

import com.discountworld.discovery.DiscoveryServiceGrpcKt
import com.discountworld.discovery.DiscoveryServiceMergedGrpcKt
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.MetadataUtils
import kotlin.io.encoding.Base64

object GrpcStubClient {
    private const val API_KEY = "8e18fcbf-bbec-4898-b142-b8ae7da90e13"
    private const val SERVER_URL = "api.appbiance.com"
    private const val SERVER_PORT = 443

    private val channel: ManagedChannel by lazy {
        OkHttpChannelBuilder
            .forAddress(SERVER_URL, SERVER_PORT)
            .useTransportSecurity()
            .build()
    }

     val stub: DiscoveryServiceGrpcKt.DiscoveryServiceCoroutineStub by lazy {
        val headers = Metadata()
        val apiKeyKey = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)
        val originalBytes = API_KEY.toByteArray(Charsets.UTF_8)
        val encodedString = Base64.Default.encode(originalBytes)
        headers.put(apiKeyKey, encodedString)

        MetadataUtils.attachHeaders(
            DiscoveryServiceGrpcKt.DiscoveryServiceCoroutineStub(channel),
            headers
        )
    }

    val stubMerge: DiscoveryServiceMergedGrpcKt.DiscoveryServiceMergedCoroutineStub by lazy {
        val headers = Metadata()
        val apiKeyKey = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER)
        val originalBytes = API_KEY.toByteArray(Charsets.UTF_8)
        val encodedString = Base64.Default.encode(originalBytes)
        headers.put(apiKeyKey, encodedString)

        MetadataUtils.attachHeaders(
            DiscoveryServiceMergedGrpcKt.DiscoveryServiceMergedCoroutineStub(channel),
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
            Result.failure(e)
        } catch (e: StatusRuntimeException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}