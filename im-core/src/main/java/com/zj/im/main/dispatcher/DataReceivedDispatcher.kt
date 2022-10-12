package com.zj.im.main.dispatcher

import com.zj.im.chat.enums.SendMsgState
import com.zj.im.chat.enums.ConnectionState
import com.zj.im.chat.modle.BaseMsgInfo
import com.zj.im.chat.modle.IMLifecycle
import com.zj.im.fetcher.BaseFetcher
import com.zj.im.utils.netUtils.NetWorkInfo
import com.zj.im.main.ChatBase
import com.zj.im.main.StatusHub
import com.zj.im.utils.cast
import com.zj.im.utils.log.logger.printInFile


@Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
internal object DataReceivedDispatcher {

    private var chatBase: ChatBase<*>? = null
    private fun getClient(case: String) = chatBase?.getClient(case)
    private fun getServer(case: String) = chatBase?.getServer(case)

    fun init(chatBase: ChatBase<*>?) {
        this.chatBase = chatBase
    }

    fun reconnect(case: String) {
        chatBase?.reconnect(case)
    }

    fun <T> pushData(data: BaseMsgInfo<T>) {
        chatBase?.enqueue(data)
    }

    fun <T> sendMsg(data: BaseMsgInfo<T>) {
        chatBase?.sendTo(data)
    }

    fun postError(throwable: Throwable?) {
        chatBase?.postError(throwable)
    }

    fun onLayerChanged(isHidden: Boolean) {
        chatBase?.onAppLayerChanged(isHidden)
    }

    fun pauseIMLooper(case: String) {
        chatBase?.pauseIMLooper(case)
    }

    fun resumeIMLooper(case: String) {
        chatBase?.pauseIMLooper(case)
    }

    fun checkNetWork(alwaysCheck: Boolean) {
        getServer("on app layer changed")?.checkNetWork(alwaysCheck)
    }

    fun isDataEnable(): Boolean {
        return StatusHub.curConnectionState.isConnected() && isNetWorkAccess()
    }

    private fun isNetWorkAccess(): Boolean {
        return getServer("check data enable")?.isNetWorkAccess == true
    }

    fun onLifeStateChanged(lifecycle: IMLifecycle) {
        chatBase?.notify("onLifecycleChanged to ${lifecycle.type.name}  by code: ${lifecycle.what}")?.onLifecycle(lifecycle)
    }

    fun onNetworkStateChanged(netWorkState: NetWorkInfo) {
        printInFile("onNetworkStateChanged", "the SDK checked the network status changed to ${if (netWorkState == NetWorkInfo.CONNECTED) "enable" else "disable"} by net State : ${netWorkState.name}")
        chatBase?.notify()?.onNetWorkStatusChanged(netWorkState)
        if ((netWorkState == NetWorkInfo.CONNECTED && StatusHub.curConnectionState.canConnect()) || netWorkState == NetWorkInfo.DISCONNECTED) {
            onConnectionStateChange(ConnectionState.OFFLINE)
        }
    }

    fun <T> sendingStateChanged(sendingState: SendMsgState, callId: String, data: T?, ignoreSendState: Boolean, resend: Boolean) {
        getClient("on sending state changed")?.onSendingStateChanged(sendingState, callId, cast(data), ignoreSendState, resend)
    }

    fun <T> received(data: T?, sendingState: SendMsgState?, callId: String) {
        sendingStateChanged(sendingState ?: SendMsgState.NONE, callId, data, ignoreSendState = false, resend = false)
    }

    fun <T> routeToClient(data: T?, pending: Any?, callId: String) {
        getClient("RouteCall")?.onRouteCall(callId, cast(data), pending)
    }

    fun <T> routeToServer(data: T?, pending: Any?, callId: String) {
        getServer("RouteCall")?.onRouteCall(callId, cast(data), pending)
    }

    fun onConnectionStateChange(connState: ConnectionState) {
        if (connState.canConnect()) {
            val reason = when (connState) {
                is ConnectionState.ERROR -> connState.reason
                is ConnectionState.RECONNECT -> connState.reason
                is ConnectionState.OFFLINE -> "net work state changed"
                else -> ""
            }
            if (connState !is ConnectionState.ERROR || connState.reconAble) {
                getServer("connect state changed to ${connState::class.java.simpleName}")?.tryToReConnect(reason)
            }
        }
        StatusHub.curConnectionState = connState
        chatBase?.notify("on connection state changed to ${connState::class.java.simpleName}")?.onConnectionStatusChanged(connState)
    }

    fun onSendingProgress(callId: String, progress: Int) {
        getClient("on sending progress update")?.progressUpdate(progress, callId)
    }

    fun getFetcherTasks(): Array<BaseFetcher>? {
        return chatBase?.getFetcherTasks()
    }
}