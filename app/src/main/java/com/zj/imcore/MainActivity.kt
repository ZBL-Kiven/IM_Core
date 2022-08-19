package com.zj.imcore

import android.app.Notification
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.zj.im.chat.core.IMOption
import com.zj.im.chat.enums.RuntimeEfficiency
import com.zj.imcore.bean.QueryInfo
import com.zj.imcore.core.IMHelper
import com.zj.imcore.utl.TestMsgTransfer

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        IMHelper.initIMSdk(IMOption.create(this.application).logsCollectionAble { true }.logsFileName(cacheDir.path).setLogsMaxRetain(86400000L).debug().setLevel(RuntimeEfficiency.HIGH).setNotify(Notification()).setSessionId(0x1).build())
        initData()
    }


    private fun initData() {
        val queryData = QueryInfo("MsgInfo", 100)
        IMHelper.addReceiveObserver<String>("MainActivity", this).withData(queryData).listen { r, lr, payload ->

        }

        /**
         * 注册监听器以得到来自 IMHelper.postToUI 发送的数据。
         * */
        val l = IMHelper.addReceiveObserver<String>("MainActivity", this) //
            .filterIn { d, pl -> return@filterIn d.isNotEmpty() && !pl.isNullOrEmpty() } //通过 数据 d 和 payloads 返回是否需要该数据 。
            .ignoreNullData(false) // 是否忽略空数据，注意，false 是不忽略空数据， 默认是 true。
            .withData(0) // 参考 1.1 withData(obj)
            .log() //是否开启此 Observer 的日志，默认关闭。
            .listen { r, lr, pl ->

                // 无 filterIn 或 filterIn 返回 true 的时候，此处会回调 postToUi 的数据。
                Log.e("---- ", "ReceiveObserver : new receive --> \nr = $r   \nlr = $lr   \npl = $pl")
            }


        /**
         *  除包含 ReceiveObserver 所有配置方法外，TransferObserver 多了以下两个配置项。
         * */
        val l1 = IMHelper.addTransferObserver<String, Int>("MainActivity", this) //
            .transform(TestMsgTransfer::class.java)  // 使用哪个类处理消息的转换，可参考 DataHandler<T>
            .filterOut { s, s1 -> //经过 DataHandler 处理后的数据，返回 false 则会丢弃。
                return@filterOut true
            }.listen { r, lr, payload ->
                // 无 filterIn / filterOut 或 filterIn & filterOut 均返回 true 的时候，此处会回调 postToUi 的数据。
            }



        l.resume()
        l.pause()
        l.shutdown()
    }
}
