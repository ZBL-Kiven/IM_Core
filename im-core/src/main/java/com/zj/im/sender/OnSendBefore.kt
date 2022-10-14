package com.zj.im.sender


/**
 * Created by ZJJ
 */
abstract class OnSendBefore<T> : OnStatus<T> {

    private lateinit var callId: String
    private lateinit var onStatus: OnPendingStatus<T>

    internal fun onCall(callId: String, data: T?, onStatus: OnPendingStatus<T>) {
        this.onStatus = onStatus
        this.callId = callId
        call(callId, data)
    }

    final override fun success(data: T) {
        onStatus.call(this.callId, data)
    }

    final override fun error(data: T?, e: Throwable?, payloadInfo: Any?) {
        onStatus.error(this.callId, data, e, payloadInfo)
    }

    final override fun onProgress(progress: Int) {
        onStatus.onProgress(this.callId, progress)
    }

    /**
     * This method will be called during the waiting period of the message queue.
     * It will not block the normal passage of the message.
     * The message will only be sent after all the [OnSendBefore] attached to this message are executed,
     * so it is very suitable for message preprocessing ,
     * additional tasks in case of message file upload, session creation before sending, etc.
     *
     * @param callId It cannot be changed during message sending, so it is passed here but rejected in super.
     *
     * Call the method on handled.
     * It needs to be called manually, as mark of the [OnStatus.onProgress],
     * it means the execution is completed, such as [OnStatus.success] [OnStatus.error]
     * */
    abstract fun call(callId: String, data: T?)

}

