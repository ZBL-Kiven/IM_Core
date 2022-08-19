package com.zj.imcore.core

import com.zj.im.chat.hub.ServerHub
import com.zj.im.chat.interfaces.SendingCallBack

class MyServer : ServerHub<String>() {

    override fun connect(connectId: String) {

    }

    override fun send(params: String, callId: String, callBack: SendingCallBack<String>): Long {
        return 0
    }

    override fun closeConnection(case: String) {

    }

    override fun pingServer(response: (Boolean) -> Unit) {
        super.pingServer(response)
    }

    override var maxPingCount: Int
        get() = super.maxPingCount
        set(value) {}

}