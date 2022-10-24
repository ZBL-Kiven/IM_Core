package com.zj.im.chat.hub

import android.app.Application
import com.zj.im.chat.enums.LifeType
import com.zj.im.chat.enums.SendMsgState
import com.zj.im.chat.interfaces.MessageInterface
import com.zj.im.chat.modle.IMLifecycle
import com.zj.im.main.StatusHub
import com.zj.im.main.dispatcher.DataReceivedDispatcher
import com.zj.im.utils.catching
import com.zj.im.utils.cusListOf
import com.zj.im.utils.log.logger.e
import com.zj.im.utils.log.logger.printInFile

/**
 * Created by ZJJ
 *
 * the bridge of client, override and custom your client hub.
 *
 * it may reconnection if change the system clock to earlier.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class ClientHub<T> {

    var context: Application? = null; internal set

    private val statusCaller = cusListOf<String>()

    /**
     * Called by the attached OnSendBefore mount task when sending a message.
     * For details, please refer to [com.zj.im.sender.OnSendBefore]
     * */
    open fun progressUpdate(progress: Int, callId: String) {}

    /**
     * all messages entry .
     * @see [com.zj.im.main.impl.RunnerClientStub.sendMsg] for Params detail.
     * @param onFinish until it called ,the next message can take out from msg queue.
     * @param isResent from resent call.
     * */
    protected open fun onMsgPatch(data: T?, callId: String?, ignoreSendState: Boolean, sendingState: SendMsgState?, isResent: Boolean, onFinish: () -> Unit) {
        postToUi(data, callId, ignoreSendState, onFinish)
    }

    /**
     * post data to UIObservers , see [com.zj.im.chat.poster.UICreator]
     * */
    protected fun postToUi(data: Any?, pl: String?, ignoreSendState: Boolean, onFinish: () -> Unit) {
        try {
            if (ignoreSendState) {
                onFinish()
            } else {
                MessageInterface.postToUIObservers(null, data, pl, onFinish)
            }
        } catch (e: Exception) {
            printInFile("client hub error ", " the ui poster throw an error case: ${e.message}")
        }
    }

    /**
     * see [com.zj.im.main.impl.IMInterface.routeToClient]
     * */
    open fun onRouteCall(callId: String?, data: T?, pending: Any?) {}

    /**
     * Mark of the data  is receive able , by the return state.
     * */
    open fun canReceived(): Boolean {
        return StatusHub.isRunning() && !StatusHub.isReceiving && DataReceivedDispatcher.isDataEnable()
    }

    /**
     * As this above, it's marks of the send state.
     * */
    open fun canSend(): Boolean {
        return StatusHub.isAlive() && DataReceivedDispatcher.isDataEnable()
    }

    internal fun onSendingStateChanged(sendingState: SendMsgState, callId: String, data: T?, ignoreSendState: Boolean, resend: Boolean) {
        onMsgPatch(data, callId, ignoreSendState, sendingState, resend) {
            StatusHub.isReceiving = false
        }
    }

    internal fun pause(code: String): Boolean {
        statusCaller.add(code)
        if (StatusHub.isPaused()) return false
        StatusHub.onLifecycle(IMLifecycle(LifeType.PAUSE, code))
        return true
    }

    internal fun resume(code: String): Boolean {
        if (!statusCaller.contains(code)) {
            e("ClientHub.resume", "the case $code is unused by func pause(), it can't resume any if queue is pausing!")
        }
        val invoke = catching {
            statusCaller.remove(code)
            if (statusCaller.isNotEmpty() || StatusHub.isRunning()) return@catching false
            StatusHub.onLifecycle(IMLifecycle(LifeType.RESUME, code));true
        }
        return invoke ?: false
    }

    internal fun shutdown() {
        StatusHub.onLifecycle(IMLifecycle(LifeType.STOP, "-- shutdown --"))
    }
}
