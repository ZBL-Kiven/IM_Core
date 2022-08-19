package com.zj.im.chat.exceptions

/**
 * created by ZJJ
 *
 * base of chat exception
 * */
open class IMException(case: String?, private val body: Any? = null, @ErrorLevel val errorLevel: String = ERROR_LEVEL_ALERT) : Throwable(case) {

    companion object {
        /**
         * Common error, it won't interfere with the SDK running state.
         * */
        const val ERROR_LEVEL_ALERT = "alert"
        /**
         * The type of error that will cause the SDK to self-check.
         * Usually, after such an error occurs or is thrown manually, it is a good choice to re-initialize the SDK.
         * */
        const val ERROR_LEVEL_DEADLY = "deadly"
        /**
         * Usually the database corruption, failed migration, file exception, notification pipeline occupation and other errors that exist at the time of compilation are configured by the host.
         * If this happens online, the user usually has to reinstall or roll back the version, so the fatal error of omission When an error that cannot be repaired independently occurs,
         * the APP is called back through such an error.
         * The developer should usually perform compatibility processing in the APP according to the specific error received.
         * For example, when such an error marked as database corruption occurs,
         * clear the local Database configuration file and re-update from remote.
         * */
        const val ERROR_LEVEL_REINSTALL = "reinstall"
    }

    @Suppress("unused")
    fun getBodyData(): Any? {
        return body
    }
}
