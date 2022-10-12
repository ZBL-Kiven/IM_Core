package com.zj.im.fetcher

abstract class FetchResultRunner : Runnable {

    internal lateinit var result: FetchResult

    abstract fun result(result: FetchResult)

    final override fun run() {
        result(result)
    }
}