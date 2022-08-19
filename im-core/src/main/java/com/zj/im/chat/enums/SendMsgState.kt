package com.zj.im.chat.enums

/**
 * created by ZJJ
 *
 * the msg sending state
 * [TIME_OUT],[FAIL] means failure.
 * [NONE] is received or your set.
 * [SUCCESS] is send or your set.
 * [ON_SEND_BEFORE_END] means a mount task ended.
 * */
@Suppress("unused")
enum class SendMsgState(val type: Int) {

    TIME_OUT(-2), FAIL(-1), NONE(0), SENDING(1), ON_SEND_BEFORE_END(2), SUCCESS(3);

    private var specialBody: Any? = null

    /**
     * The parameter of the mount task [com.zj.im.sender.OnSendBefore]
     * [com.zj.im.sender.OnStatus] is the parameter that is actively delivered by the implementer when OnError is executed,
     * [specialBody] is usually used to return some special The logical parameter of processing
     * or business failure is convenient for special processing when [com.zj.im.chat.hub.ClientHub.onMsgPatch] is called.
     * */
    fun setSpecialBody(specialBody: Any?): SendMsgState {
        this.specialBody = specialBody
        return this
    }

    fun peekSpecialBody(): Any? {
        return specialBody
    }

    fun getSpecialBody(): Any? {
        val sb = specialBody
        this.specialBody = null
        return sb
    }

    companion object {
        fun parseStateByType(type: Int?): SendMsgState? {
            var state: SendMsgState? = null
            values().forEach {
                if (it.type == type) {
                    state = it
                    return@forEach
                }
            }
            return state
        }
    }
}