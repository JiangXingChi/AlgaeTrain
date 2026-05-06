package com.rhodes.algae.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rhodes.algae.data.AlgaeItem
import com.rhodes.algae.viewmodel.TrainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val APP_VERSION = "V0.1.4"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: TrainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(when (selectedTab) {
                    0 -> "🌿 识藻训练"
                    1 -> "📋 错题集"
                    else -> "ℹ️ 关于"
                }, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary))
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, "训练") },
                    label = { Text("识藻训练") })
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, "错题集") },
                    label = { Text("错题集") })
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Info, "关于") },
                    label = { Text("关于") })
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> TrainContent(vm)
                1 -> ErrorBookContent(vm)
                2 -> AboutContent()
            }
        }
    }
}

// ── 训练页 ──

@Composable
private fun TrainContent(vm: TrainViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        StatsBar(vm)
        Spacer(Modifier.height(8.dp))
        val total = vm.poolTotal(); val done = vm.poolDone()
        if (total > 0) {
            LinearProgressIndicator(
                { done.toFloat() / total },
                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.weight(1f)) {
            val item = vm.currentItem
            if (item != null) FlashcardView(vm, item)
            else Column(Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text("🎉", fontSize = 48.sp)
                Text("本轮已全部完成！", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("已掌握 ${vm.knownCount} / ${vm.allItems.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.restart() }) { Text("再来一轮") }
            }
        }
    }
}

// ── 关于页 ──

@Composable
private fun AboutContent() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // 玩法说明
        Card(shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp)) {
                Text("📖 玩法说明", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("本工具用于训练淡水藻类识别能力，数据集包含 8 门 124 属共 629 张显微图片。支持中英双语拉丁学名对照。",
                    lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                InstructionStep("1", "看图片，判断是否认识这种藻类")
                InstructionStep("2", "双指缩放观察细节，单击卡牌翻面查看名称")
                InstructionStep("3", "认识点 ✓ 认识，不认识点 ✗ 不认识")
                InstructionStep("4", "点错了？点击底部蓝色「↩ 撤销上一张」返回重标")
                InstructionStep("5", "不认识的项目会自动加入错题集")
                InstructionStep("6", "随时切换底部 📋 错题集 标签页复习薄弱项")
                InstructionStep("7", "系统自动断点续练，退出后进度保留")
            }
        }

        // 版本信息
        Card(shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp)) {
                Text("📱 版本信息", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                InfoRow("当前版本", APP_VERSION)
                InfoRow("最低系统", "Android 8.0+")
                InfoRow("数据规模", "8 门 · 124 属 · 629 图")
                InfoRow("技术栈", "Kotlin + Jetpack Compose")
            }
        }

        // 开发团队
        Card(shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp)) {
                Text("👥 开发团队", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                DevRow("喝茶喵", "项目负责人")
                DevRow("凯尔希", "架构 & AI 工程")
                DevRow("蛋蛋", "数据 & 标注")
                DevRow("进宝", "测试 & 运维")
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("🤖 AI 支持", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                Text("• DeepSeek V4 Pro — 代码生成与逻辑推理",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• Hermes Agent — 自动化构建与部署",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("🎨 应用图标", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                Text("• 图标由千问（Qwen）生成",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // 图片来源与版权
        Card(shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp)) {
                Text("📸 图片来源与版权", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("本应用图片素材来源于以下公开出版物：",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("📗 《中国内陆水域常见藻类图谱》", fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("📘 《澳门常见淡水藻类图谱》", fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                Text("在此向两书的作者和出版方致以诚挚的感谢。",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("⚠️ 版权声明", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text("• 图片版权归原作者及出版社所有。",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• 本应用仅用于学术教育目的，完全免费且开源。",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• 严禁将本应用中的图片用于任何商业用途。",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• 如您是版权方且认为本应用侵犯了您的权益，请通过以下邮箱联系，我们将在 48 小时内处理。",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• 建议用户购买正版图谱书籍以支持原创作者。",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("📧 pandalinux@163.com", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary)
            }
        }

        // 开源许可
        Card(shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp)) {
                Text("📜 开源许可", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("本软件采用 MIT 开源许可协议。",
                    fontWeight = FontWeight.Bold, lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("MIT 许可证是业界最宽松的开源协议之一：",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("✅ 允许", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32))
                Text("• 自由使用、复制、修改、合并、出版发行、散布、再授权及贩售本软件",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("⚠️ 条件", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100))
                Text("• 在所有副本或重要部分中必须包含版权声明和本许可声明",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("🚫 免责", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error)
                Text("• 本软件按「原样」提供，不提供任何形式的明示或暗示担保",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("• 作者或版权持有人不对任何索赔、损害或其他责任负责",
                    lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("完整许可证文本详见项目根目录 LICENSE 文件。",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Copyright © 2025 喝茶喵 & Contributors",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InstructionStep(num: String, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) {
            Text(num,
                fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, Modifier.weight(1f), lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DevRow(name: String, role: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Surface(shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(role, Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

// ── 复用组件 ──

@Composable
fun StatsBar(vm: TrainViewModel) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surface).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceAround) {
        StatItem(vm.seenCount.toString(), "本轮已看")
        StatItem(vm.knownCount.toString(), "已掌握")
        StatItem((vm.allItems.size - vm.knownCount).toString(), "未掌握")
        StatItem("${vm.poolDone()}/${vm.poolTotal()}", "进度")
    }
}

@Composable
fun StatItem(num: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FlashcardView(vm: TrainViewModel, item: AlgaeItem) {
    val known = vm.isKnown(item.id)
    val ctx = LocalContext.current
    var bitmap by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showRestartConfirm by remember { mutableStateOf(false) }
    var zoomScale by remember(item.id) { mutableFloatStateOf(1f) }
    var zoomOffsetX by remember(item.id) { mutableFloatStateOf(0f) }
    var zoomOffsetY by remember(item.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(item.id) {
        withContext(Dispatchers.IO) {
            try { ctx.assets.open("images/${item.file}").use { bitmap = BitmapFactory.decodeStream(it) } }
            catch (_: Exception) {}
        }
    }
    Column(Modifier.fillMaxSize()) {
        Card(Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Box(Modifier.fillMaxSize()) {
                if (!vm.flipped) {
                    Box(Modifier.fillMaxSize().clipToBounds()) {
                        bitmap?.let {
                            Image(it.asImageBitmap(), item.id,
                                Modifier.fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomScale, scaleY = zoomScale,
                                        translationX = zoomOffsetX, translationY = zoomOffsetY),
                                contentScale = ContentScale.Fit)
                        }
                        ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator() }
                        // gesture overlay — separate layer avoids conflicts
                        Box(Modifier.fillMaxSize()
                            .pointerInput(item.id) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                    zoomScale = newScale
                                    if (newScale > 1.01f) {
                                        zoomOffsetX += pan.x
                                        zoomOffsetY += pan.y
                                    } else {
                                        zoomOffsetX = 0f; zoomOffsetY = 0f
                                    }
                                }
                            }
                            .pointerInput(item.id) {
                                detectTapGestures(
                                    onTap = { if (zoomScale <= 1.01f) vm.flip() },
                                    onDoubleTap = { zoomScale = 1f; zoomOffsetX = 0f; zoomOffsetY = 0f }
                                )
                            }
                        )
                    }
                    Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        shape = RoundedCornerShape(8.dp), color = Color(0x73000000)) {
                        Text(if (zoomScale > 1.01f) "👆 双击还原" else "👆 点击翻面 · 双指缩放",
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp, color = Color.White)
                    }
                } else {
                    Column(Modifier.fillMaxSize()
                        .clickable { vm.flip() }
                        .background(Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer))),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Text(item.phylum, fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.phylumLatin.isNotEmpty()) {
                            Text(item.phylumLatin, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.genus, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        if (item.genusLatin.isNotEmpty()) {
                            Text(item.genusLatin, fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                        Spacer(Modifier.height(12.dp))
                        Surface(shape = RoundedCornerShape(10.dp),
                            color = if (known) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)) {
                            Text(if (known) "✓ 已掌握" else "○ 未掌握",
                                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = if (known) Color(0xFF2E7D32) else Color(0xFFE65100))
                        }
                        Spacer(Modifier.height(16.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0x73000000)) {
                            Text("👆 点击翻转", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (vm.canUndo) {
            TextButton(onClick = { vm.undo() }, Modifier.fillMaxWidth()) {
                Text("↩ 撤销上一张", fontSize = 14.sp, color = Color(0xFF1565C0))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ vm.mark(true) }, Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)) {
                Text("✓ 认识", Modifier.padding(vertical = 8.dp), fontSize = 16.sp) }
            Button({ vm.mark(false) }, Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                shape = RoundedCornerShape(12.dp)) {
                Text("✗ 不认识", Modifier.padding(vertical = 8.dp), fontSize = 16.sp) }
            OutlinedButton({ showRestartConfirm = true }, shape = RoundedCornerShape(12.dp)) {
                Text("重新开始", Modifier.padding(vertical = 8.dp), fontSize = 14.sp) }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (showRestartConfirm) {
        AlertDialog(
            onDismissRequest = { showRestartConfirm = false },
            title = { Text("确认重新开始") },
            text = { Text("当前进度和记住的答案将丢失，确定要重新开始吗？") },
            confirmButton = { TextButton({ vm.restart(); showRestartConfirm = false }) { Text("确定") } },
            dismissButton = { TextButton({ showRestartConfirm = false }) { Text("取消") } })
    }
}

@Composable
fun ErrorBookContent(vm: TrainViewModel) {
    val entries = remember(vm.errorBookVersion) { vm.errorBookEntries() }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    if (selectedIndex >= 0 && selectedIndex < entries.size) {
            val item = entries[selectedIndex].first
            val errCount = entries[selectedIndex].second
            val hasPrev = selectedIndex > 0
            val hasNext = selectedIndex < entries.size - 1
            var bitmap by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(item.id) {
                withContext(Dispatchers.IO) {
                    try { ctx.assets.open("images/${item.file}").use { bitmap = BitmapFactory.decodeStream(it) } }
                    catch (_: Exception) {}
                }
            }
            // zoom state
            var zoomScale by remember(item.id) { mutableFloatStateOf(1f) }
            var zoomOffsetX by remember(item.id) { mutableFloatStateOf(0f) }
            var zoomOffsetY by remember(item.id) { mutableFloatStateOf(0f) }
            Card(Modifier.fillMaxSize().padding(16.dp), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { selectedIndex = -1 }) { Text("← 返回列表") }
                        Spacer(Modifier.weight(1f))
                        Text("${item.genus} #${item.number}  (${selectedIndex + 1}/${entries.size})",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()
                        .pointerInput(item.id) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                zoomScale = newScale
                                if (newScale > 1.01f) {
                                    zoomOffsetX += pan.x
                                    zoomOffsetY += pan.y
                                } else {
                                    zoomOffsetX = 0f; zoomOffsetY = 0f
                                }
                            }
                        }
                        .pointerInput(item.id) {
                            detectTapGestures(onDoubleTap = {
                                zoomScale = 1f; zoomOffsetX = 0f; zoomOffsetY = 0f
                            })
                        },
                        contentAlignment = Alignment.Center) {
                        bitmap?.let {
                            Image(it.asImageBitmap(), item.id,
                                Modifier.fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomScale, scaleY = zoomScale,
                                        translationX = zoomOffsetX, translationY = zoomOffsetY),
                                contentScale = ContentScale.Fit)
                        } ?: CircularProgressIndicator()
                        Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            shape = RoundedCornerShape(8.dp), color = Color(0x73000000)) {
                            Text(if (zoomScale > 1.01f) "👆 双击还原" else "👆 双指缩放",
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp, color = Color.White)
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${item.phylum} · ${item.genus}", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        if (item.phylumLatin.isNotEmpty() || item.genusLatin.isNotEmpty()) {
                            val latin = listOfNotNull(
                                item.phylumLatin.ifEmpty { null },
                                item.genusLatin.ifEmpty { null }
                            ).joinToString(" · ")
                            Text(latin, fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                        Spacer(Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.error) {
                            Text("错误 ${errCount} 次", Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onError, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // 翻页按钮
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { if (hasPrev) selectedIndex-- },
                            enabled = hasPrev) {
                            Text(if (hasPrev) "◀ 上一张" else "◀ 上一张",
                                color = if (hasPrev) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                        TextButton(onClick = { if (hasNext) selectedIndex++ },
                            enabled = hasNext) {
                            Text(if (hasNext) "下一张 ▶" else "下一张 ▶",
                                color = if (hasNext) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        } else {
            Card(Modifier.fillMaxSize().padding(16.dp), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxSize()) {
                    Text("📋 错题集", Modifier.padding(16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (entries.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无错题 🎉", textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            itemsIndexed(entries) { idx, (item, count) ->
                                Row(Modifier.fillMaxWidth().clickable { selectedIndex = idx }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${item.genus} #${item.number}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${item.phylum}", fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.error) {
                                        Text("$count 次", Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = {
                            if (entries.isNotEmpty()) showClearConfirm = true
                        }) {
                            Text("🗑 清空错题集", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
        }
    }
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空错题集") },
            text = { Text("将清除所有题目的错误记录，此操作不可撤销。确定吗？") },
            confirmButton = {
                TextButton({
                    vm.clearErrors()
                    showClearConfirm = false
                }) { Text("确定清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ showClearConfirm = false }) { Text("取消") } })
    }
}
