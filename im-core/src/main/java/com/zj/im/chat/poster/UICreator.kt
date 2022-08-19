package com.zj.im.chat.poster

import androidx.lifecycle.LifecycleOwner


@Suppress("unused", "MemberVisibilityCanBePrivate")
sealed class UICreator<T : Any, R : Any> constructor(val uniqueCode: Any, private val lifecycleOwner: LifecycleOwner? = null, val inCls: Class<T>, val outerCls: Class<R>, val innerCls: Class<*>? = null) {

    var withData: Any? = null
    internal var filterIn: ((T, String?) -> Boolean)? = null
    internal var filterOut: ((R, String?) -> Boolean)? = null
    internal lateinit var inObserver: ObserverIn
    internal var ignoreNullData: Boolean = true
    internal var logAble = false
    private var onDataReceived: ((R?, List<R>?, String?) -> Unit)? = null
    private var isPaused: Boolean = false
    private val cacheData = hashSetOf<CacheData<R>>()
    private var options: UIOptions<T, R>? = null
    internal var handlerCls: Class<*>? = null

    internal fun obs(inObserver: ObserverIn) {
        this.inObserver = inObserver
    }

    fun withData(withData: Any?): UICreator<T, R> {
        this.withData = withData
        return this
    }

    fun ignoreNullData(ignore: Boolean): UICreator<T, R> {
        this.ignoreNullData = ignore
        return this
    }

    fun log(): UICreator<T, R> {
        logAble = true;return this
    }

    fun listen(onDataReceived: (r: R?, lr: List<R>?, payload: String?) -> Unit): UICreator<T, R> {
        this.onDataReceived = onDataReceived
        options = UIOptions(uniqueCode, lifecycleOwner, this, inObserver) { d, s, pl ->
            cacheData.add(CacheData(d, s, pl))
            if (!isPaused) {
                notifyDataChanged()
            }
        }
        return this
    }

    private fun notifyDataChanged() {
        cacheData.forEach {
            onDataReceived?.invoke(it.d, it.lst, it.payload)
        }
        cacheData.clear()
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
        notifyDataChanged()
    }

    fun shutdown() {
        options?.destroy()
        cacheData.clear()
        options = null
        onDataReceived = null
        isPaused = false
        filterIn = null
        filterOut = null
    }
}