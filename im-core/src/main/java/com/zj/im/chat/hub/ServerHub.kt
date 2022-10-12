package com.zj.im.chat.hub

import android.app.Application
import com.zj.im.chat.enums.ConnectionState
import com.zj.im.chat.exceptions.IMException
import com.zj.im.chat.modle.BaseMsgInfo
import com.zj.im.chat.interfaces.SendingCallBack
import com.zj.im.main.StatusHub
import com.zj.im.main.dispatcher.DataReceivedDispatcher
import com.zj.im.main.looper.MsgExecutor
import com.zj.im.main.looper.MsgHandlerQueue
import com.zj.im.utils.Constance
import com.zj.im.utils.log.logger.NetRecordUtils
import com.zj.im.utils.log.logger.printErrorInFile
import com.zj.im.utils.log.logger.printInFile
import com.zj.im.utils.netUtils.IConnectivityManager
import com.zj.im.utils.netUtils.NetWorkInfo
import com.zj.im.utils.nio
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


@Suppress("unused", "SameParameterValue", "MemberVisibilityCanBePrivate")
abstract class ServerHub<T> constructor(private var isAlwaysHeartBeats: Boolean = false) {

    companion object {
        private const val HEART_BEATS_BASE_TIME = 3000L
        private const val CONNECT_STATE_CHANGE = 0xf1379
        var currentConnectId: String = ""; private set
    }

    protected var app: Application? = null
    private var connectivityManager: IConnectivityManager? = null
    private var alwaysHeartbeats: AtomicBoolean = AtomicBoolean(isAlwaysHeartBeats)
    private var pingHasNotResponseCount = 0
    private var pongTime = 0L
    private var pingTime = 0L
    open val maxPingCount = 5
    open val heartbeatsIncrease = 1.5f
    open val heartbeatsAttenuate = 0.2f
    open val heartbeatsMaxInterval = HEART_BEATS_BASE_TIME * 10f
    private var curPingCount: AtomicInteger = AtomicInteger(0)
    private var heartbeatsTime = HEART_BEATS_BASE_TIME
    open val reconnectionTime = Constance.DEFAULT_RECONNECT_TIME
    private var handler: MsgExecutor? = null

    val isNetWorkAccess: Boolean
        get() {
            return connectivityManager?.isNetWorkActive == NetWorkInfo.CONNECTED
        }

    /**
     * Implement the message send.
     * @see [com.zj.im.main.impl.RunnerClientStub.sendMsg] for callId detail.
     * @param callBack until it called ,the next message can take out from msg queue.
     * */
    protected abstract fun send(params: T, callId: String, callBack: SendingCallBack<T>): Long

    /**
     * Close the connection, this behavior will not make it reconnect,
     * if there is no downstream reconnect operation.
     * */
    protected abstract fun closeConnection(case: String)

    /**
     * Called when a connection is required, this behavior may occur during initialization,
     * or when reconnection, PostError handling needs to be reconnected。
     * */
    protected abstract fun connect(connectId: String)

    open fun init(context: Application?) {
        this.app = context
        connectivityManager = IConnectivityManager()
        connectivityManager?.init(context) { netWorkStateChanged(it) }
    }

    /**
     * see [com.zj.im.main.impl.IMInterface.routeToServer]
     * */
    open fun onRouteCall(callId: String?, data: T?, pending: Any?) {}

    /**
     * This method will be called when the kernel judges that it needs to try to reconnect after a network disconnection,
     * self-test needs to be repaired, etc., and it will still be called to [connect] in the end.
     * */
    open fun onReConnect(case: String) {
        printErrorInFile("ServerHub.onReconnect", case, true)
        handler?.enqueue(ConnectionState.CONNECTION(true), reconnectionTime)
    }

    /**
     * Commend to override it, Implement the Ping Frame to server in actually.
     * */
    open fun pingServer(response: (Boolean) -> Unit) {
        response(isNetWorkAccess)
    }

    /**
     * Handle current server-related state or changes in MsgThread.
     * The necessary processing has been included by default,
     * which can effectively prevent the Service from being called or accessed on the wrong thread after the downstream override.
     * */
    private fun onHandlerExecute(what: Int, obj: Any?) {
        if (obj is ConnectionState) {
            when (obj) {
                is ConnectionState.INIT -> {
                }
                is ConnectionState.CONNECTION -> {
                    curConnectionState = ConnectionState.CONNECTION(true)
                    currentConnectId = UUID.randomUUID().toString()
                    try {
                        connect(currentConnectId)
                    } catch (e: Exception) {
                        printInFile("ServerHub.connect", "${Constance.CONNECT_ERROR}${e.message}", true)
                        tryToReConnect("Error:${e.message}")
                    }
                }
                is ConnectionState.PING -> {
                    onCalledPing()
                }
                is ConnectionState.PONG -> {
                    onCalledPong()
                }
                is ConnectionState.CONNECTED -> {
                    onCalledConnected(obj.fromReconnect)
                }
                is ConnectionState.OFFLINE, is ConnectionState.ERROR, is ConnectionState.RECONNECT -> {
                    onCalledError(obj)
                }
            }
        } else onMsgThreadCallback(what, obj)
    }

    /**
     * You called [sendToMsgThread] , so you have receiving the callback on here.
     * the thread-current is MsgThread.
     * */
    open fun onMsgThreadCallback(what: Int, obj: Any?) {}

    /**
     * Post a cmd with [what] for msg thread , it'll call at a suit time ,in [onMsgThreadCallback].
     * */
    protected fun sendToMsgThread(what: Int, delay: Long, obj: Any?) {
        handler?.enqueue(what, delay, obj)
    }

    /**
     * Remove a msg before it execute.
     * */
    protected fun removeFromMsgThread(what: Int) {
        handler?.removeMessages(what)
    }

    /**
     * Don't forget call if you are sure it connected.
     * */
    protected fun postOnConnected() {
        handler?.enqueue(ConnectionState.CONNECTED(false))
    }

    /**
     * Actively calls and ends the connection, it eventually calls [closeConnection]
     * */
    protected fun postToClose(case: String, reconAble: Boolean) {
        handler?.enqueue(ConnectionState.ERROR(case, reconAble))
    }

    protected fun postError(case: String) {
        postError(IMException(case))
    }

    protected fun postError(throws: Throwable?) {
        val reconAble = if (throws is IMException) {
            throws.errorLevel == IMException.ERROR_LEVEL_ALERT
        } else {
            throws !is RuntimeException
        }
        postToClose(throws?.message ?: "UN_KNOW_ERROR", reconAble)
        throws?.let { DataReceivedDispatcher.postError(it) }
    }

    protected fun isConnected(): Boolean {
        return curConnectionState.isConnected()
    }

    internal fun checkNetWork(alwaysCheck: Boolean) {
        this.alwaysHeartbeats.set(this.isAlwaysHeartBeats || alwaysCheck)
        nextHeartbeats(true)
    }

    internal fun tryToReConnect(case: String) {
        NetRecordUtils.recordDisconnectCount()
        onReConnect(case)
    }

    internal fun sendToServer(params: T, callId: String, callBack: SendingCallBack<T>) {
        val size = send(params, callId, callBack)
        if (size > 0) NetRecordUtils.recordLastModifySendData(size)
    }

    internal fun isDataEnable(): Boolean {
        return StatusHub.curConnectionState.isConnected() && isNetWorkAccess
    }

    internal fun onLooperPrepared(queue: MsgHandlerQueue) {
        if (handler == null) handler = MsgExecutor(queue, ::onHandlerExecute)
        handler?.enqueue(ConnectionState.CONNECTION(false))
    }

    private fun netWorkStateChanged(state: NetWorkInfo) {
        DataReceivedDispatcher.pushData(BaseMsgInfo.networkStateChanged<T>(state))
    }

    /**
     * @param isSpecialData This message is prioritized when calculating priority and is not affected by pauses
     * */
    @Suppress("SameParameterValue")
    protected fun postReceivedMessage(callId: String, data: T, isSpecialData: Boolean, size: Long) {
        if (size > 0) NetRecordUtils.recordLastModifyReceiveData(size)
        DataReceivedDispatcher.pushData(BaseMsgInfo.receiveMsg(callId, data, isSpecialData))
    }

    protected fun recordOtherSendNetworkDataSize(size: Long) {
        if (size > 0) NetRecordUtils.recordLastModifySendData(size)
    }

    protected fun recordOtherReceivedNetworkDataSize(size: Long) {
        if (size > 0) NetRecordUtils.recordLastModifyReceiveData(size)
    }

    private fun nextHeartbeats(resetPingCount: Boolean) {
        if (resetPingCount) curPingCount.set(0)
        handler?.enqueue(ConnectionState.PING, heartbeatsTime)
    }

    private var curConnectionState: ConnectionState = ConnectionState.INIT
        set(value) {
            if (value != field) {
                field = value
                if (value.isValidState()) DataReceivedDispatcher.pushData<T>(BaseMsgInfo.connectStateChange(value))
                when (value) {
                    is ConnectionState.PING -> printInFile("on connection status change with id: $currentConnectId", "--- ${value::class.java.simpleName} -- ${nio(pingTime)}")
                    is ConnectionState.PONG -> printInFile("on connection status change with id: $currentConnectId", "--- ${value::class.java.simpleName} -- ${nio(pongTime)}")
                    is ConnectionState.ERROR -> printInFile("on connection status change with id: $currentConnectId", "${value::class.java.simpleName}  ==> reconnection with error : ${value.reason}")
                    else -> printInFile("on connection status change with id: $currentConnectId", "--- $value --")
                }
            }
        }
        get() {
            synchronized(field) {
                return field
            }
        }

    private fun onCalledPing() {
        if (!alwaysHeartbeats.get()) curPingCount.addAndGet(1)
        if (pingTime > 0 && pongTime <= 0) {
            pingHasNotResponseCount++
        }
        if (pingHasNotResponseCount > 3) {
            postToClose(Constance.PING_TIMEOUT, true)
            clearPingRecord()
            return
        } else {
            pingServer {
                if (!it) postToClose("PING sent with server received explicit error callback, see [pingServer(Result)]!", true)
                else {
                    handler?.enqueue(ConnectionState.PONG)
                }
            }
            val inc = if (pongTime < 0) {
                (heartbeatsTime * heartbeatsAttenuate).coerceAtLeast(100f)
            } else {
                ((heartbeatsTime.coerceAtLeast(HEART_BEATS_BASE_TIME)) * heartbeatsIncrease).coerceAtMost(heartbeatsMaxInterval)
            }
            heartbeatsTime = inc.toLong()
            if (alwaysHeartbeats.get() || curPingCount.get() < maxPingCount) {
                nextHeartbeats(false)
            }
        }
        pingTime = System.currentTimeMillis()
        pongTime = -1
        curConnectionState = ConnectionState.PING
    }

    private fun onCalledPong() {
        pingHasNotResponseCount = 0
        pongTime = System.currentTimeMillis()
        curConnectionState = ConnectionState.PONG
    }

    private fun onCalledConnected(fromReconnected: Boolean) {
        curConnectionState = ConnectionState.CONNECTED(fromReconnected)
        clearPingRecord()
        if (alwaysHeartbeats.get() || curPingCount.get() < maxPingCount) nextHeartbeats(false)
    }

    private fun onCalledError(state: ConnectionState) {
        clearPingRecord()
        curConnectionState = state
    }

    private fun clearPingRecord() {
        pongTime = 0L
        pingTime = 0L
        curPingCount.set(0)
        pingHasNotResponseCount = 0
        heartbeatsTime = HEART_BEATS_BASE_TIME
        handler?.remove(ConnectionState.PING)
        handler?.remove(ConnectionState.PONG)
    }

    open fun shutdown() {
        closeConnection("shutdown")
        handler?.clearAndDrop()
        curConnectionState = ConnectionState.INIT
        connectivityManager?.shutDown()
    }
}