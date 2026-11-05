package com.blueeve.aikeypro;

/** 模块与 system_server 共享的配置键（remote prefs 组名 "aikeypro"）。 */
public final class Prefs {

    public static final String GROUP = "aikeypro";

    public static final String SHORT_APP = "short_app";
    public static final String SHORT_TYPE = "short_type";
    public static final String SHORT_DATA = "short_data";

    public static final String LONG_APP = "long_app";
    public static final String LONG_TYPE = "long_type";
    public static final String LONG_DATA = "long_data";

    /** 自定义动作类型 */
    public static final String TYPE_APP = "app";
    public static final String TYPE_URL = "url";
    public static final String TYPE_ACTIVITY = "activity";
    public static final String TYPE_BROADCAST = "broadcast";
    public static final String TYPE_SHELL = "shell";

    public static String appKey(boolean longPress) {
        return longPress ? LONG_APP : SHORT_APP;
    }

    public static String typeKey(boolean longPress) {
        return longPress ? LONG_TYPE : SHORT_TYPE;
    }

    public static String dataKey(boolean longPress) {
        return longPress ? LONG_DATA : SHORT_DATA;
    }

    private Prefs() {
    }
}
