package com.zj.im.fetcher

import com.zj.im.chat.enums.ConnectionState
import com.zj.im.main.dispatcher.DataReceivedDispatcher
import com.zj.im.main.impl.IMInterface
import com.zj.im.utils.MainLooper
import com.zj.im.utils.log.logger.d
import com.zj.im.utils.log.logger.printInFile

internal object Fetcher {

    private const val OFFLINE_FETCHER = "offline_fetcher_task"
    private var FETCH_CODE_INCREMENT: Int = 0
        get() {
            return field++
        }
    private val cachedResultListener = mutableMapOf<String, MutableMap<Int, FetchResultRunner>>()

    fun init(imInterface: IMInterface<*>) {
        imInterface.registerConnectionStateChangeListener("offline_fetcher_observer") {
            val tasks = DataReceivedDispatcher.getFetcherTasks()
            if (tasks.isNullOrEmpty()) return@registerConnectionStateChangeListener
            when (it) {
                is ConnectionState.CONNECTED -> {
                    DataReceivedDispatcher.pauseIMLooper(OFFLINE_FETCHER)
                    BaseFetcher.startFetch() //NotificationFetcher
                }
                is ConnectionState.ERROR, is ConnectionState.OFFLINE -> {
                    cancelAll()
                }
                else -> {
                }
            }
        }
    }

    fun refresh(f: BaseFetcher, result: FetchResultRunner) {
        fun pushFetcher(code: Int) {
            val cache = cachedResultListener[f.getPayload()]
            if (cache == null) {
                cachedResultListener[f.getPayload()] = mutableMapOf()
                pushFetcher(code)
            } else {
                cache[code] = result
            }
        }

        val code = FETCH_CODE_INCREMENT
        synchronized(this) {
            pushFetcher(code)
            BaseFetcher.refresh(code, f)
        }
    }

    fun notifyNodeEnd(prop: FetchType, result: FetchResult?) {
        d("Fetcher", " Fetch node end with : ${prop.dealCls?.getPayload()}!!")
        if (result != null) endOfRefresh(prop, result, false)
    }

    fun endOfFetch(prop: FetchType, result: FetchResult?) {
        d("Fetcher", " Fetch finished with last : ${prop.dealCls?.getPayload()}!!")
        if (result != null) {
            endOfRefresh(prop, result, false)
        } else {
            DataReceivedDispatcher.resumeIMLooper(OFFLINE_FETCHER)
        }
    }

    fun endOfRefresh(prop: FetchType, result: FetchResult, formRefresh: Boolean = true) {
        prop.fetchIds.forEach {
            val r = cachedResultListener.remove(prop.dealCls?.getPayload())?.get(it) ?: return@forEach
            r.result = result
            MainLooper.post(r)
        }
        if (formRefresh) {
            d("Fetcher", "on refresh result : ${result.success}")
        }
    }

    fun cancelAll() {
        BaseFetcher.cancelAll()
    }

    fun resetIncrementTsForProp(prop: FetchType) {
        printInFile("Fetcher.resetIncrementTsForProp", "pl -> ${prop.dealCls?.getPayload()} has been fetch error, try to pull again by earlier ts?")
    }
}