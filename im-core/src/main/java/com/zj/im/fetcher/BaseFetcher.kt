package com.zj.im.fetcher

import com.zj.im.main.dispatcher.DataReceivedDispatcher
import com.zj.im.utils.catching
import com.zj.im.utils.log.logger.e
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingDeque
import kotlin.coroutines.resume

abstract class BaseFetcher {

    abstract fun getPayload(): String
    abstract suspend fun startFetch(): FetchResult

    private var selfInFetching = false
    private var reqProp: FetchType? = null

    companion object {
        private var fetching = false

        private val cachedFetchers = LinkedBlockingDeque<FetchType>()

        fun cancelAll() {
            catching {
                cachedFetchers.forEach { it.compo?.cancel(null) }
                cachedFetchers.clear()
            }
        }

        fun startFetch(vararg from: BaseFetcher) {
            if (fetching) return
            fetching = true
            from.forEach { getOrCreateFetcher(it, null, FetchType.FETCH_FLAG_PENDING) }
            fetch()
        }

        fun refresh(fetchId: Int, vararg f: BaseFetcher) {
            f.forEach { getOrCreateFetcher(it, fetchId, FetchType.FETCH_FLAG_REFRESH) }
            fetch()
        }

        private fun fetch() {
            val prop = cachedFetchers.poll()
            if (prop == null) {
                Fetcher.endOfFetch(FetchType(), null);return
            }
            if (prop.dealCls?.selfInFetching == true) return
            prop.dealCls?.selfInFetching = true
            catching({
                prop.dealCls?.let {
                    it.reqProp = prop
                    MainScope().launch {
                        val r = CoroutineScope(Dispatchers.IO).async {
                            suspendCancellableCoroutine { sc ->
                                prop.compo = sc
                                catching {
                                    this.launch {
                                        sc.resume(it.startFetch())
                                    }
                                }
                            }
                        }
                        prop.dealCls?.finishFetch(prop, r.await())
                    }
                }
            }, {
                prop.dealCls?.selfInFetching = false
            })
        }

        private fun getOrCreateFetcher(from: BaseFetcher, fetchId: Int?, flags: Int): FetchType {
            var f = cachedFetchers.firstOrNull { it.dealCls?.equals(from) == true }
            if (f == null) {
                f = FetchType()
                cachedFetchers.offer(f)
            }
            if (f.dealCls != from) {
                f.dealCls?.cancel()
                f.dealCls = from
            }
            f.flags = f.flags or flags
            if (fetchId != null) f.fetchIds.add(fetchId)
            return f
        }
    }

    protected fun cancel(e: Throwable? = null) {
        reqProp?.compo?.cancel(e)
        if (e != null) {
            fetching = false
            selfInFetching = false
            cachedFetchers.clear()
            DataReceivedDispatcher.postError(e)
            DataReceivedDispatcher.reconnect("fetch failed , case : Fetcher was canceled by error ${e.message}!!")
        }
    }

    private fun finishFetch(prop: FetchType, result: FetchResult) {
        selfInFetching = false
        if (prop.flags.and(FetchType.FETCH_FLAG_PENDING) != 0) {
            if (result.success) {
                if (cachedFetchers.all { prop.flags.and(FetchType.FETCH_FLAG_PENDING) == 0 }) {
                    fetching = false
                    selfInFetching = false
                    Fetcher.endOfFetch(prop, result)
                } else {
                    Fetcher.notifyNodeEnd(prop, result)
                    fetch()
                }
            } else {
                fetching = false
                Fetcher.resetIncrementTsForProp(prop)
                DataReceivedDispatcher.postError(result.e)
                DataReceivedDispatcher.reconnect("fetch failed , case : fetch ${prop.dealCls?.getPayload()} failed with : ${result.e?.message} !!")
                e("BaseFetcher.finishAFetch", "Tips:\nYou can make [FetchResult] params returned a special exception,\nexample:[IMException] to identify some type, and deal it in yor IMInterface Extender!")
            }
        }
        if (prop.flags.and(FetchType.FETCH_FLAG_REFRESH) != 0) {
            Fetcher.endOfRefresh(prop, result)
        }
    }
}