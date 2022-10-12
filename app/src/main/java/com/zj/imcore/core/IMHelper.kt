package com.zj.imcore.core

import com.zj.im.chat.exceptions.IMException
import com.zj.im.chat.hub.ClientHub
import com.zj.im.chat.hub.ServerHub
import com.zj.im.chat.poster.UICreator
import com.zj.im.fetcher.BaseFetcher
import com.zj.im.main.impl.IMInterface
import com.zj.imcore.bean.QueryInfo


/**
 * call [send] to send a message.
 * */
object IMHelper : IMInterface<String>() {

    /**
     * 这里返回你定义的 Client（？ super ClientHub）,
     * Client 是你用来接收消息并处理的接口 , 具体可参考 [ClientHub]
     * */
    override fun getClient(): ClientHub<String> {
        return MyClient()
    }

    override fun getServer(): ServerHub<String> {
        return MyServer()
    }

    override fun onError(e: IMException) {

    }

    override fun onSdkDeadlyError(e: IMException) {

    }

    override fun onNewListenerRegistered(creator: UICreator<*, *>) {
        (creator.withData as? QueryInfo?)?.let {  //也可以通过 creator.uniqueCode 精确判断是哪个监听器。
            when (it.table) {
                "MsgInfo" -> { // 这里为了保证数据的操作原子性，建议 Route 到 MsgThread 进行处理（推荐）。
                    routeToClient("CMD_TO_GET_MSG", it.table)
                }
            }
            routeToUi("", "", "")
        }
        super.onNewListenerRegistered(creator)
    }
}