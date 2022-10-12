package com.zj.im.fetcher

import java.io.Serializable

data class FetchResult(val success: Boolean, val isFirstFetch: Boolean, val isNullData: Boolean, val e: Throwable? = null) : Serializable