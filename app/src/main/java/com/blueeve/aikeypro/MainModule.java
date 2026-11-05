package com.blueeve.aikeypro;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * AI 键增强 —— LSPosed 入口。
 *
 * system_server 侧三层 hook：
 *  1. AppStatusCheck.needFallbackToNone   阻止自定义值被重置为 none
 *  2. HandleMiAiKey.getClickFunction/getLongPressFunction  让白名单外的值通过
 *  3. MiAiKeyRule.triggerFunction         接管自定义/扩展动作的分发
 *
 * com.miui.securitycore 侧：
 *  4. GestureShortcutAiKeySettingsActivity$a.onCreatePreferences  向下拉框注入选项
 */
public class MainModule extends XposedModule {

    private static final String TAG = "AIKeyPro";
    private static final String PKG_SETTINGS = "com.miui.securitycore";
    private static final String CLS_RULE =
            "com.android.server.input.shortcut.singlekeyrule.MiAiKeyRule";
    private static final String CLS_HANDLE = CLS_RULE + "$HandleMiAiKey";
    private static final String CLS_APP_STATUS = CLS_HANDLE + "$AppStatusCheck";
    private static final String CLS_SETTINGS_FRAGMENT =
            "com.miui.miinput.gesture.aikey.GestureShortcutAiKeySettingsActivity$a";

    private ClassLoader mServerLoader;
    private Field mContextField;
    private Field mClickFunctionField;
    private Field mLongPressFunctionField;
    private Method mScauGetInstance;
    private Method mScauTrigger;

    // ---------------------------------------------------------------- entry

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        mServerLoader = param.getClassLoader();
        try {
            hookFallbackBlocker();
            hookFunctionGetters();
            hookRuleTrigger();
            log(Log.INFO, TAG, "system_server hooks installed");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "install system_server hooks failed", t);
        }
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        if (!PKG_SETTINGS.equals(param.getPackageName())) {
            return;
        }
        try {
            Class<?> fragment = Class.forName(CLS_SETTINGS_FRAGMENT, false, param.getClassLoader());
            Method onCreate = fragment.getDeclaredMethod("onCreatePreferences", Bundle.class, String.class);
            hook(onCreate).intercept(new XposedInterface.Hooker() {
                @Override
                public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    try {
                        injectDropdowns(chain.getThisObject());
                    } catch (Throwable t) {
                        log(Log.ERROR, TAG, "inject dropdowns failed", t);
                    }
                    return result;
                }
            });
            log(Log.INFO, TAG, "settings UI hook installed");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "install settings UI hook failed", t);
        }
    }

    // ------------------------------------------------------- system_server

    /** 1. 系统会把白名单外的设置值强制写回 none，这里放行模块支持的值。 */
    private void hookFallbackBlocker() throws Exception {
        Class<?> appStatus = Class.forName(CLS_APP_STATUS, false, mServerLoader);
        Method method = appStatus.getDeclaredMethod("needFallbackToNone", String.class, String.class);
        hook(method).intercept(new XposedInterface.Hooker() {
            @Override
            public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                String function = (String) chain.getArg(0);
                String action = (String) chain.getArg(1);
                if (isAiKeyAction(action) && Actions.isManaged(function)) {
                    return null;
                }
                return chain.proceed();
            }
        });
    }

    /** 2. 让 getClickFunction/getLongPressFunction 原样返回模块支持的值。 */
    private void hookFunctionGetters() throws Exception {
        Class<?> handle = Class.forName(CLS_HANDLE, false, mServerLoader);
        mClickFunctionField = handle.getDeclaredField("mClickMiAiKeyFunction");
        mClickFunctionField.setAccessible(true);
        mLongPressFunctionField = handle.getDeclaredField("mLongPressMiAiKeyFunction");
        mLongPressFunctionField.setAccessible(true);

        hookGetter(handle, "getClickFunction", mClickFunctionField);
        hookGetter(handle, "getLongPressFunction", mLongPressFunctionField);
    }

    private void hookGetter(Class<?> handle, String name, final Field field) throws Exception {
        Method method = handle.getDeclaredMethod(name);
        hook(method).intercept(new XposedInterface.Hooker() {
            @Override
            public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                Object raw = field.get(chain.getThisObject());
                if (raw instanceof String && Actions.isManaged((String) raw)) {
                    return raw;
                }
                return chain.proceed();
            }
        });
    }

    /** 3. 接管 MiAiKeyRule.triggerFunction 的分发。 */
    private void hookRuleTrigger() throws Exception {
        Class<?> rule = Class.forName(CLS_RULE, false, mServerLoader);
        mContextField = rule.getDeclaredField("mContext");
        mContextField.setAccessible(true);

        Method trigger = rule.getDeclaredMethod("triggerFunction", String.class, String.class);
        hook(trigger).intercept(new XposedInterface.Hooker() {
            @Override
            public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                String function = (String) chain.getArg(0);
                String action = (String) chain.getArg(1);
                if (isAiKeyAction(action) && Actions.isManaged(function)) {
                    dispatch(chain.getThisObject(), function, action);
                    return null;
                }
                return chain.proceed();
            }
        });
    }

    private void dispatch(Object rule, String function, String action) {
        try {
            Context context = (Context) mContextField.get(rule);
            boolean longPress = "long_press_mi_ai_key".equals(action);

            if (Actions.EXT_LAUNCH_APP.equals(function)) {
                SharedPreferences prefs = getRemotePreferences(Prefs.GROUP);
                String pkg = prefs.getString(Prefs.appKey(longPress), null);
                if (pkg != null && !pkg.isEmpty()) {
                    Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } else {
                        log(Log.WARN, TAG, "no launch intent for " + pkg);
                    }
                }
                return;
            }

            if (Actions.EXT_CUSTOM.equals(function)) {
                SharedPreferences prefs = getRemotePreferences(Prefs.GROUP);
                String type = prefs.getString(Prefs.typeKey(longPress), Prefs.TYPE_URL);
                String data = prefs.getString(Prefs.dataKey(longPress), "");
                executeCustom(context, type, data);
                return;
            }

            // 复用系统 ShortCutActionsUtils 已实现的动作
            ensureScau();
            Object instance = mScauGetInstance.invoke(null, context);
            mScauTrigger.invoke(instance, function, action, null, true);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "dispatch " + function + " failed", t);
        }
    }

    private void ensureScau() throws Exception {
        if (mScauTrigger != null) {
            return;
        }
        Class<?> scau = Class.forName("com.miui.server.input.util.ShortCutActionsUtils",
                false, mServerLoader);
        mScauGetInstance = scau.getMethod("getInstance", Context.class);
        mScauTrigger = scau.getMethod("triggerFunction",
                String.class, String.class, Bundle.class, boolean.class);
    }

    private void executeCustom(Context context, String type, String data) {
        if (data == null || data.isEmpty()) {
            log(Log.WARN, TAG, "custom action has no data");
            return;
        }
        try {
            switch (type) {
                case Prefs.TYPE_URL: {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    break;
                }
                case Prefs.TYPE_ACTIVITY: {
                    ComponentName cn = ComponentName.unflattenFromString(data);
                    if (cn == null) {
                        log(Log.WARN, TAG, "bad component: " + data);
                        return;
                    }
                    Intent intent = new Intent();
                    intent.setComponent(cn);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    break;
                }
                case Prefs.TYPE_BROADCAST: {
                    Intent intent = new Intent(data);
                    context.sendBroadcast(intent);
                    break;
                }
                case Prefs.TYPE_SHELL: {
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", data});
                    break;
                }
                default:
                    log(Log.WARN, TAG, "unknown custom type: " + type);
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "execute custom " + type + " failed", t);
        }
    }

    private static boolean isAiKeyAction(String action) {
        return "click_mi_ai_key".equals(action) || "long_press_mi_ai_key".equals(action);
    }


    // ---------------------------------------------------------- 设置页 UI

    private void injectDropdowns(Object fragment) throws Exception {
        for (String name : new String[]{"c", "d"}) {
            Field field = fragment.getClass().getField(name);
            Object preference = field.get(fragment);
            if (preference != null) {
                appendEntries(preference);
            }
        }
    }

    private void appendEntries(Object preference) throws Exception {
        Field entriesField = preference.getClass().getField("o");
        Field valuesField = preference.getClass().getField("p");
        CharSequence[] entries = (CharSequence[]) entriesField.get(preference);
        CharSequence[] values = (CharSequence[]) valuesField.get(preference);
        if (entries == null || values == null) {
            return;
        }

        List<CharSequence> newEntries = new ArrayList<>(Arrays.asList(entries));
        List<CharSequence> newValues = new ArrayList<>(Arrays.asList(values));
        for (Map.Entry<String, String> item : Actions.ALL.entrySet()) {
            if (newValues.contains(item.getKey())) {
                continue;
            }
            newValues.add(item.getKey());
            newEntries.add(item.getValue());
        }

        Method setEntries = preference.getClass().getMethod("h", CharSequence[].class);
        Method setEntryValues = preference.getClass().getMethod("i", CharSequence[].class);
        setEntries.invoke(preference, (Object) newEntries.toArray(new CharSequence[0]));
        setEntryValues.invoke(preference, (Object) newValues.toArray(new CharSequence[0]));
    }
}
