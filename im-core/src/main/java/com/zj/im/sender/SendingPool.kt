package com.zj.im.sender

import com.zj.im.chat.enums.SendMsgState
import com.zj.im.chat.exceptions.IMException
import com.zj.im.chat.modle.BaseMsgInfo
import com.zj.im.chat.modle.SendingUp
import com.zj.im.main.dispatcher.DataReceivedDispatcher
import com.zj.im.utils.cusListOf
import com.zj.im.utils.log.logger.printInFile

/**
 * Created by ZJJ
 */
internal class SendingPool<T> : OnPendingStatus<T> {

    private var sending = false

    private val pendingTaskQueue = cusListOf<BaseMsgInfo<T>>()

    fun push(info: BaseMsgInfo<T>) {
        pendingTaskQueue.add(info)
        if (info.onSendBefore.isNullOrEmpty().not()) {
            info.onSendBefore?.poll()?.onCall(info.callId, info.data, this)
        }
    }

    fun lock() {
        sending = true
    }

    fun unLock() {
        sending = false
    }

    fun pop(): BaseMsgInfo<T>? {
        if (sending) return null
        if (pendingTaskQueue.isEmpty()) return null
        if (!DataReceivedDispatcher.isDataEnable()) {
            val grouped = pendingTaskQueue.group {
                it.ignoreConnecting
            } ?: return null
            pendingTaskQueue.clear()
            pendingTaskQueue.addAll(grouped[true])
            grouped[false]?.forEach { ds ->
                ds.joinInTop = true
                DataReceivedDispatcher.pushData(ds)
            }
            if (pendingTaskQueue.isEmpty()) return null
        }
        var firstInStay = pendingTaskQueue.getFirst()
        if (firstInStay?.sendingUp == SendingUp.WAIT) {
            firstInStay = pendingTaskQueue.getFirst {
                it.sendingUp == SendingUp.NORMAL
            }
        }
        firstInStay?.let {
            pendingTaskQueue.remove(it)
            return it
        }
        return null
    }

    fun deleteFormQueue(callId: String?) {
        callId?.let {
            pendingTaskQueue.removeIf { m ->
                m.callId == callId
            }
        }
    }

    fun queryInSendingQueue(predicate: (BaseMsgInfo<T>) -> Boolean): Boolean {
        return pendingTaskQueue.contains(predicate)
    }

    fun clear() {
        pendingTaskQueue.clear()
        sending = false
    }

    override fun call(callId: String, data: T) {
        printInFile("SendExecutors.send", "$callId before sending task success")
        pendingTaskQueue.getFirst { obj -> obj.callId == callId }?.apply {
            this.data = data
            if (onSendBefore.isNullOrEmpty()) {
                this.sendingUp = SendingUp.READY
                val sendState = SendMsgState.ON_SEND_BEFORE_END
                val notifyState = BaseMsgInfo.sendingStateChange(sendState, callId, data, isResend, sendWithoutState)
                DataReceivedDispatcher.pushData(notifyState)
            } else {
                this.onSendBefore?.poll()?.onCall(this.callId, data, this@SendingPool)
            }
        }
    }

    override fun error(callId: String, data: T?, e: Throwable?, payloadInfo: Any?) {
        if (e != null) {
            val ime = if (e is IMException) {
                val body = e.getBodyData()
                IMException("SendExecutors.send $callId before sending task error,case:\n${e.message}\n payload = $payloadInfo", body)
            } else {
                IMException("SendExecutors.send $callId before sending task error,case:\n${e.message}\n payload = $payloadInfo", e)
            }
            DataReceivedDispatcher.postError(ime)
        }
        pendingTaskQueue.getFirst { obj -> obj.callId == callId }?.apply {
            if (data != null) this.data = data
            this.sendingUp = SendingUp.CANCEL
            this.onSendBefore?.clear()
            this.onSendBefore = null
            this.sendingState = SendMsgState.FAIL.setSpecialBody(payloadInfo)
        }
    }

    override fun onProgress(callId: String, progress: Int) {
        pendingTaskQueue.getFirst { obj -> obj.callId == callId }?.apply {
            customSendingCallback?.let {
                it.onSendingUploading(progress, sendWithoutState, callId)
                if (it.pending) DataReceivedDispatcher.pushData(BaseMsgInfo.onProgressChange<T>(progress, callId))
            } ?: DataReceivedDispatcher.pushData(BaseMsgInfo.onProgressChange<T>(progress, callId))
        }
    }
}
