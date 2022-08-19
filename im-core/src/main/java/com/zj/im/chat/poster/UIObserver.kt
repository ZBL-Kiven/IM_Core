@file:Suppress("unused")

package com.zj.im.chat.poster

import androidx.lifecycle.LifecycleOwner

class UIObserver<T : Any> internal constructor(inObserver: ObserverIn, uniqueCode: Any, lo: LifecycleOwner?, inCls: Class<T>, outCls: Class<T>, innerCls: Class<*>? = null) : UICreator<T, T>(uniqueCode, lo, inCls, outCls, innerCls) {

    init {
        obs(inObserver)
    }

    fun filterIn(filter: (T, String?) -> Boolean): UIObserver<T> {
        this.filterIn = filter
        return this
    }
}

class UITransferObserver<T : Any, R : Any> internal constructor(inObserver: ObserverIn, uniqueCode: Any, lo: LifecycleOwner?, inCls: Class<T>, outCls: Class<R>, innerCls: Class<*>? = null) : UICreator<T, R>(uniqueCode, lo, inCls, outCls, innerCls) {

    init {
        obs(inObserver)
    }

    fun <L : DataHandler<T>> transform(handlerCls: Class<L>): UITransferObserver<T, R> {
        this.handlerCls = handlerCls
        return this
    }

    fun filterIn(filter: (T, String?) -> Boolean): UITransferObserver<T, R> {
        this.filterIn = filter
        return this
    }

    fun filterOut(filter: (R, String?) -> Boolean): UITransferObserver<T, R> {
        this.filterOut = filter
        return this
    }

}