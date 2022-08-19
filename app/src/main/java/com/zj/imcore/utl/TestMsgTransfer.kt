package com.zj.imcore.utl

import com.zj.im.chat.poster.DataHandler

class TestMsgTransfer : DataHandler<String> {

    /**
     * @return 在这里处理一条消息的时候，可以返回更多的消息或者类型完全不同的消息 ，
     * 返回的数据，对于触发此函数的 Observer 来说，是 out 数据。
     * 对于其他类型不满足触发此函数的 Observer 设定的类型时，对其他支持此类型的 Observer 来说，是 in 数据。
     * */
    override fun handle(data: String, pl: String?): MutableList<Pair<Any?, String?>> {
        val result = mutableListOf<Pair<Any?, String?>>()

        result.add(Pair("把原始数据改变 ，data = $data", pl)) //改变原数据

        // 如下，可以新抛送一个其他任何类型，比如说 Int ，就可以被指定接收 Int 的 Observer 收到。
        result.add(Pair(0, pl))

        // 当然也可以返回空数组，这样等同于丢弃了。
        return result
    }
}