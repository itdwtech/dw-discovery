package com.discountworld.easypaisasdk.utils

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.util.ContentLengthInputStream
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Configures Glide once to use a shared OkHttpClient with HTTP/2 multiplexing and
 * connection pooling so concurrent Home Page images don't stall on separate TCP/TLS handshakes.
 */
object GlideImageLoader {

    @Volatile
    private var isConfigured = false

    private val okHttpClient: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 12
        }
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun ensureConfigured(context: Context) {
        if (isConfigured) return
        synchronized(this) {
            if (isConfigured) return
            Glide.get(context.applicationContext).registry.replace(
                GlideUrl::class.java,
                InputStream::class.java,
                OkHttpUrlLoaderFactory(okHttpClient)
            )
            isConfigured = true
        }
    }

    private class OkHttpUrlLoaderFactory(
        private val client: Call.Factory
    ) : ModelLoaderFactory<GlideUrl, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> {
            return OkHttpUrlLoader(client)
        }

        override fun teardown() = Unit
    }

    private class OkHttpUrlLoader(
        private val client: Call.Factory
    ) : ModelLoader<GlideUrl, InputStream> {
        override fun buildLoadData(
            model: GlideUrl,
            width: Int,
            height: Int,
            options: Options
        ): ModelLoader.LoadData<InputStream> {
            return ModelLoader.LoadData(model, OkHttpStreamFetcher(client, model))
        }

        override fun handles(model: GlideUrl): Boolean = true
    }

    private class OkHttpStreamFetcher(
        private val client: Call.Factory,
        private val url: GlideUrl
    ) : DataFetcher<InputStream>, Callback {

        @Volatile
        private var call: Call? = null
        private var stream: InputStream? = null
        private var responseBody: ResponseBody? = null
        private var callback: DataFetcher.DataCallback<in InputStream>? = null

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
            this.callback = callback
            val requestBuilder = Request.Builder().url(url.toStringUrl())
            url.headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
            val newCall = client.newCall(requestBuilder.build())
            call = newCall
            newCall.enqueue(this)
        }

        override fun onFailure(call: Call, e: IOException) {
            callback?.onLoadFailed(e)
        }

        override fun onResponse(call: Call, response: Response) {
            responseBody = response.body
            if (response.isSuccessful) {
                val body = responseBody
                if (body == null) {
                    callback?.onLoadFailed(HttpException("Empty response body", response.code))
                    return
                }
                val contentLength = body.contentLength()
                stream = ContentLengthInputStream.obtain(body.byteStream(), contentLength)
                callback?.onDataReady(stream)
            } else {
                callback?.onLoadFailed(HttpException(response.message, response.code))
            }
        }

        override fun cleanup() {
            try {
                stream?.close()
            } catch (_: IOException) {
            }
            responseBody?.close()
            callback = null
        }

        override fun cancel() {
            call?.cancel()
        }

        override fun getDataClass(): Class<InputStream> = InputStream::class.java

        override fun getDataSource(): DataSource = DataSource.REMOTE
    }
}

