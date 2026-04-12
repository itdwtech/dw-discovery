package com.discountworld.easypaisasdk.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object CoroutineTask {

    fun<T: Any> ioThenMain(work : suspend (() -> T?), callback:((T?) -> Unit)) =
        CoroutineScope(Dispatchers.Main).launch {
            val data = try {
                CoroutineScope(Dispatchers.IO).async rt@{
                    return@rt work()
                }.await()
            } catch (e: Exception) {
                null
            }
            callback(data)
        }

    fun main(work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Main).launch {
            work()
        }

    fun<T: Any> global(work : suspend (() -> T?), callback:((T?) -> Unit)) =
        GlobalScope.launch(Dispatchers.IO) {
            val data = CoroutineScope(Dispatchers.IO).async rt@{
                return@rt work()
            }.await()
            callback(data)
        }

    fun io(work: suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.IO).launch {
            work()
        }

    fun<T: Any> ioThenResult(work : suspend (() -> T?), callback:((T?) -> Unit)) =
        CoroutineScope(Dispatchers.IO).launch {
            val data = CoroutineScope(Dispatchers.IO).async rt@{
                return@rt work()
            }.await()
            callback(data)
        }
}
