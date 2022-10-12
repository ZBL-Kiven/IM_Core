package com.zj.im.fetcher

import kotlinx.coroutines.CancellableContinuation


internal class FetchType {

    internal companion object {
        const val FETCH_FLAG_PENDING = 1
        const val FETCH_FLAG_REFRESH = 2
    }

    var fetchIds = mutableSetOf<Int>()
    var flags: Int = 0
    var dealCls: BaseFetcher? = null
    var compo: CancellableContinuation<*>? = null

    override fun equals(other: Any?): Boolean {
        return (other is FetchType) && other.dealCls == dealCls
    }

    override fun hashCode(): Int {
        return dealCls?.hashCode() ?: 0
    }
}