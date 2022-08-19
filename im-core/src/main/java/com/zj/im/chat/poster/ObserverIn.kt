package com.zj.im.chat.poster

internal interface ObserverIn {

    fun onObserverRegistered(creator: UICreator<*, *>)

    fun onObserverUnRegistered(creator: UICreator<*, *>)
}