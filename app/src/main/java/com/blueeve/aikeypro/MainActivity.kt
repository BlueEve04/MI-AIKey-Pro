package com.blueeve.aikeypro

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

private data class AppEntry(val pkg: String, val label: String)

private val CUSTOM_TYPES = listOf(
    Prefs.TYPE_URL to "打开网址",
    Prefs.TYPE_ACTIVITY to "打开页面",
    Prefs.TYPE_BROADCAST to "发送广播",
    Prefs.TYPE_SHELL to "执行 Shell",
)

private fun typeLabel(type: String): String =
    CUSTOM_TYPES.firstOrNull { it.first == type }?.second ?: "打开网址"

private fun typeHint(type: String): String = when (type) {
    Prefs.TYPE_URL -> "https://example.com"
    Prefs.TYPE_ACTIVITY -> "com.example/.MainActivity"
    Prefs.TYPE_BROADCAST -> "com.example.ACTION_DO_SOMETHING"
    Prefs.TYPE_SHELL -> "am start -n com.example/.MainActivity"
    else -> ""
}

@Composable
private fun App() {
    val context = LocalContext.current
    val store = remember { ConfigStore(context) }
    var version by remember { mutableIntStateOf(0) }

    var pickerTarget by remember { mutableStateOf<Boolean?>(null) }
    var editorTarget by remember { mutableStateOf<Boolean?>(null) }
    var editorType by remember { mutableStateOf(Prefs.TYPE_URL) }
    var editorData by remember { mutableStateOf("") }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadLauncherApps(context) }
    }

    val dark = isSystemInDarkTheme()
    MiuixTheme(colors = if (dark) darkColorScheme() else lightColorScheme()) {
        // 依赖 version 触发重新读取
        val shortApp = remember(version) { store.app(false) }
        val longApp = remember(version) { store.app(true) }
        val shortType = remember(version) { store.type(false) }
        val longType = remember(version) { store.type(true) }
        val shortData = remember(version) { store.data(false) }
        val longData = remember(version) { store.data(true) }

        Scaffold(
            topBar = { SmallTopAppBar(title = "AI 键增强") },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card {
                    BasicComponent(
                        title = "短按 AI 键",
                        summary = "在系统「AI 键」设置里选择动作后生效",
                    )
                    BasicComponent(
                        title = "启动应用",
                        summary = appSummary(context, shortApp),
                        onClick = { pickerTarget = false },
                    )
                    BasicComponent(
                        title = "自定义动作",
                        summary = customSummary(shortType, shortData),
                        onClick = {
                            editorType = shortType
                            editorData = shortData
                            editorTarget = false
                        },
                    )
                }

                Card {
                    BasicComponent(
                        title = "长按 AI 键",
                        summary = "在系统「AI 键」设置里选择动作后生效",
                    )
                    BasicComponent(
                        title = "启动应用",
                        summary = appSummary(context, longApp),
                        onClick = { pickerTarget = true },
                    )
                    BasicComponent(
                        title = "自定义动作",
                        summary = customSummary(longType, longData),
                        onClick = {
                            editorType = longType
                            editorData = longData
                            editorTarget = true
                        },
                    )
                }

                Card {
                    BasicComponent(
                        title = "怎么使用",
                        summary = "打开 设置 → AI 键，把短按/长按设为「启动应用（模块内配置）」或" +
                            "「完全自定义（模块内配置）」。其余新增选项（手电筒、相机、截屏等）可直接选择。",
                    )
                }

                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.avatar),
                            contentDescription = "头像",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "AI 键增强",
                                style = MiuixTheme.textStyles.subtitle,
                            )
                            Text(
                                text = "BlueEve · v$versionName",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            )
                            Text(
                                text = "HyperOS 4 · LSPosed (libxposed API 101)",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            )
                        }
                    }
                    BasicComponent(
                        title = "酷安",
                        summary = "@BlueEve · coolapk.com/u/27414574",
                        onClick = { openUrl(context, "https://www.coolapk.com/u/27414574") },
                    )
                    BasicComponent(
                        title = "GitHub",
                        summary = "github.com/BlueEve04",
                        onClick = { openUrl(context, "https://github.com/BlueEve04") },
                    )
                }
            }

            // ---------------- 应用选择 ----------------
            val pickerLong = pickerTarget == true
            OverlayBottomSheet(
                title = if (pickerLong) "长按 · 启动应用" else "短按 · 启动应用",
                show = pickerTarget != null,
                onDismissRequest = { pickerTarget = null },
            ) {
                Card(modifier = Modifier.padding(bottom = 16.dp)) {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        items(apps) { entry ->
                            BasicComponent(
                                title = entry.label,
                                summary = entry.pkg,
                                onClick = {
                                    store.setApp(pickerLong, entry.pkg)
                                    version++
                                    pickerTarget = null
                                },
                            )
                        }
                    }
                }
            }

            // ---------------- 自定义动作编辑 ----------------
            val editorLong = editorTarget == true
            OverlayBottomSheet(
                title = if (editorLong) "长按 · 自定义动作" else "短按 · 自定义动作",
                show = editorTarget != null,
                onDismissRequest = { editorTarget = null },
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Card {
                        CUSTOM_TYPES.forEach { (type, label) ->
                            BasicComponent(
                                title = label,
                                summary = if (editorType == type) "已选择" else typeHint(type),
                                onClick = { editorType = type },
                            )
                        }
                    }

                    TextField(
                        value = editorData,
                        onValueChange = { editorData = it },
                        label = typeHint(editorType),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = { editorTarget = null },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = "保存",
                            onClick = {
                                store.setType(editorLong, editorType)
                                store.setData(editorLong, editorData.trim())
                                version++
                                editorTarget = null
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun appSummary(context: android.content.Context, pkg: String?): String {
    if (pkg.isNullOrEmpty()) return "未选择"
    return try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        pkg
    }
}

private fun customSummary(type: String, data: String): String {
    if (data.isEmpty()) return "未设置"
    return "${typeLabel(type)}：$data"
}

private fun loadLauncherApps(context: android.content.Context): List<AppEntry> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .mapNotNull { it.activityInfo?.applicationInfo }
        .distinctBy { it.packageName }
        .map { info ->
            AppEntry(info.packageName, pm.getApplicationLabel(info).toString())
        }
        .sortedBy { it.label.lowercase() }
}
