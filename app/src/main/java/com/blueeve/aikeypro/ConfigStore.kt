package com.blueeve.aikeypro

import android.content.Context

/** 与 system_server 端 getRemotePreferences("aikeypro") 对应的本地配置。 */
class ConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences(Prefs.GROUP, Context.MODE_PRIVATE)

    fun app(longPress: Boolean): String? = prefs.getString(Prefs.appKey(longPress), null)

    fun setApp(longPress: Boolean, pkg: String) {
        prefs.edit().putString(Prefs.appKey(longPress), pkg).apply()
    }

    fun type(longPress: Boolean): String =
        prefs.getString(Prefs.typeKey(longPress), Prefs.TYPE_URL) ?: Prefs.TYPE_URL

    fun setType(longPress: Boolean, type: String) {
        prefs.edit().putString(Prefs.typeKey(longPress), type).apply()
    }

    fun data(longPress: Boolean): String =
        prefs.getString(Prefs.dataKey(longPress), "") ?: ""

    fun setData(longPress: Boolean, data: String) {
        prefs.edit().putString(Prefs.dataKey(longPress), data).apply()
    }
}
