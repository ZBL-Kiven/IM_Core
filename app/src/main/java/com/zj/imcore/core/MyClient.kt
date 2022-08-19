package com.zj.imcore.core

import com.zj.im.chat.enums.SendMsgState
import com.zj.im.chat.hub.ClientHub

class MyClient : ClientHub<String>() {

    //默认
    override fun onMsgPatch(data: String?, callId: String?, ignoreSendState: Boolean, sendingState: SendMsgState?, isResent: Boolean, onFinish: () -> Unit) {

    }

    override fun onRouteCall(callId: String?, data: String?, pending: Any?) {

    }
}