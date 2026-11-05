package com.blueeve.aikeypro;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 注入到系统 AI 键设置页的动作清单。
 *
 * 前 7 项是系统原生支持的动作；其余是系统 ShortCutActionsUtils 已实现、
 * 但 AI 键白名单没有开放的动作，由模块放开并转发。
 */
public final class Actions {

    public static final String EXT_LAUNCH_APP = "ext_launch_app";
    public static final String EXT_CUSTOM = "ext_custom";

    /** value -> 显示名，顺序即下拉框顺序。 */
    public static final Map<String, String> ALL = new LinkedHashMap<>();

    static {
        // 系统原生
        ALL.put("launch_ai_memory", "超级小爱记忆");
        ALL.put("launch_voice_assistant", "小爱同学");
        ALL.put("launch_voice_note", "语音笔记");
        ALL.put("launch_peep_protection", "窥屏防护");
        ALL.put("mute", "静音");
        ALL.put("zen_mode", "勿扰模式");
        ALL.put("none", "无");

        // 模块扩展（复用系统实现）
        ALL.put("turn_on_torch", "手电筒");
        ALL.put("launch_camera", "相机");
        ALL.put("launch_camera_and_take_photo", "拍照");
        ALL.put("launch_camera_capture", "相机（拍照模式）");
        ALL.put("launch_camera_video", "相机（录像模式）");
        ALL.put("screen_shot", "截屏");
        ALL.put("partial_screen_shot", "区域截屏");
        ALL.put("launch_calculator", "计算器");
        ALL.put("launch_alipay_scanner", "支付宝扫码");
        ALL.put("launch_alipay_payment_code", "支付宝付款码");
        ALL.put("launch_alipay_bus_code", "支付宝乘车码");
        ALL.put("launch_wechat_scanner", "微信扫码");
        ALL.put("launch_wechat_payment_code", "微信付款码");
        ALL.put("launch_wechat_bus_code", "微信乘车码");
        ALL.put("launch_recents", "最近任务");
        ALL.put("launch_home", "回到桌面");
        ALL.put("go_to_sleep", "睡眠");
        ALL.put("launch_control_center", "控制中心");
        ALL.put("launch_notification_center", "通知中心");
        ALL.put("launch_screen_recorder", "屏幕录制");
        ALL.put("launch_sound_recorder", "录音机");
        ALL.put("launch_mi_creation", "小米创作");
        ALL.put("launch_xiaoai_screen_identification", "小爱识屏");
        ALL.put("launch_smarthome", "智能家居");
        ALL.put("launch_global_search", "全局搜索");
        ALL.put("launch_ai_shortcut", "AI 快捷功能");

        // 模块内配置
        ALL.put(EXT_LAUNCH_APP, "启动应用（模块内配置）");
        ALL.put(EXT_CUSTOM, "完全自定义（模块内配置）");
    }

    public static boolean isManaged(String function) {
        return function != null && ALL.containsKey(function);
    }

    private Actions() {
    }
}
