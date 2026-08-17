package com.rhodes.algae.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rhodes.algae.data.AlgaeItem
import com.rhodes.algae.ui.theme.ThemeState
import com.rhodes.algae.viewmodel.TrainViewModel

private const val VERSION = "V0.3.1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: TrainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var zoomState by remember { mutableStateOf<Pair<List<Pair<AlgaeItem, Int>>, Int>?>(null) }
    val cs = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(when (tab) { 0 -> "🌊 识浮游"; 1 -> "📋 错题"; else -> "ℹ️ 关于" },
                        fontWeight = FontWeight.Bold) },
                    actions = { ThemeToggle(cs) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = cs.primary, titleContentColor = cs.onPrimary)
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(Icons.Filled.PlayArrow, "训练") }, label = { Text("训练") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, "错题") }, label = { Text("错题") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                        icon = { Icon(Icons.Filled.Info, "关于") }, label = { Text("关于") })
                }
            },
            containerColor = cs.background
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    0 -> TrainTab(vm)
                    1 -> ErrorBookTab(vm, onZoom = { entries, idx ->
                        zoomState = Pair(entries, idx) })
                    2 -> AboutTab(vm)
                }
            }
        }

        // Fullscreen overlay — covers entire screen including system bars
        zoomState?.let { (entries, idx) ->
            FullscreenZoom(entries, idx, vm.displayPrefix) { zoomState = null }
        }
    }
}

@Composable
private fun ThemeToggle(cs: androidx.compose.material3.ColorScheme) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Settings, "主题", tint = cs.onPrimary) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("🌓 跟随系统") }, onClick = {
                ThemeState.apply(ThemeState.Mode.Auto); expanded = false })
            DropdownMenuItem(text = { Text("☀️ 白昼") }, onClick = {
                ThemeState.apply(ThemeState.Mode.Light); expanded = false })
            DropdownMenuItem(text = { Text("🌙 黑夜") }, onClick = {
                ThemeState.apply(ThemeState.Mode.Dark); expanded = false })
        }
    }
}

// ═══════════ 训练 Tab ═══════════

@Composable
private fun TrainTab(vm: TrainViewModel) {
    val cs = MaterialTheme.colorScheme
    var showRestart by remember { mutableStateOf(false) }

    if (vm.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        // Mode toggle
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = vm.currentMode == "algae", onClick = { vm.switchMode("algae") },
                label = { Text("🌿 浮游植物") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp))
            FilterChip(selected = vm.currentMode == "zooplankton", onClick = { vm.switchMode("zooplankton") },
                label = { Text("🦠 浮游动物") }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp))
        }
        Spacer(Modifier.height(8.dp))

        // Stats
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceVariant).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround) {
            StatItem(vm.seenCount.toString(), "已看", cs)
            StatItem(vm.knownCount.toString(), "掌握", cs)
            StatItem((vm.allItems.size - vm.knownCount).toString(), "未掌握", cs)
            StatItem("${vm.poolDone()}/${vm.poolTotal()}", "进度", cs)
        }
        Spacer(Modifier.height(8.dp))

        // Progress bar
        val total = vm.poolTotal(); val done = vm.poolDone()
        if (total > 0) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator({ done.toFloat() / total },
                    Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = cs.primary, trackColor = cs.surfaceVariant)
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showRestart = true }) { Text("重来", fontSize = 13.sp) }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Card
        Box(Modifier.weight(1f)) {
            val item = vm.currentItem
            if (item != null) FlashcardView(vm, item)
            else CompleteView(vm)
        }
    }

    if (showRestart) AlertDialog(
        onDismissRequest = { showRestart = false },
        title = { Text("确认重新开始") },
        text = { Text("进度和已掌握的答案将丢失，确定？") },
        confirmButton = { TextButton({ vm.restart(); showRestart = false }) { Text("确定") } },
        dismissButton = { TextButton({ showRestart = false }) { Text("取消") } })
}

@Composable
private fun StatItem(num: String, label: String, cs: androidx.compose.material3.ColorScheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cs.primary)
        Text(label, fontSize = 11.sp, color = cs.outline)
    }
}

@Composable
private fun CompleteView(vm: TrainViewModel) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("🎉", fontSize = 48.sp)
        Text("本轮全部完成！", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cs.primary)
        Spacer(Modifier.height(4.dp))
        Text("已掌握 ${vm.knownCount}/${vm.allItems.size}", color = cs.outline)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.restart() }) { Text("再来一轮") }
    }
}

@Composable
private fun FlashcardView(vm: TrainViewModel, item: AlgaeItem) {
    val cs = MaterialTheme.colorScheme
    val known = vm.isKnown(item.id)

    Column(Modifier.fillMaxSize()) {
        Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surface)) {
            Box(Modifier.fillMaxSize()) {
                if (!vm.flipped) {
                    var zoomScale by remember { mutableFloatStateOf(1f) }
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var offsetY by remember { mutableFloatStateOf(0f) }
                    Box(Modifier.fillMaxSize().clipToBounds()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("file:///android_asset/${vm.displayPrefix}${item.file}")
                                .crossfade(true).build(),
                            contentDescription = item.genus,
                            modifier = Modifier.fillMaxSize()
                                .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale,
                                    translationX = offsetX, translationY = offsetY),
                            contentScale = ContentScale.Fit)
                        Box(Modifier.fillMaxSize()
                            .pointerInput(item.id) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val s = (zoomScale * zoom).coerceIn(1f, 5f)
                                    zoomScale = s
                                    if (s > 1.01f) { offsetX += pan.x; offsetY += pan.y }
                                    else { offsetX = 0f; offsetY = 0f }
                                }
                            }
                            .pointerInput(item.id) {
                                detectTapGestures(
                                    onTap = { if (zoomScale <= 1.01f) vm.flip() },
                                    onDoubleTap = { zoomScale = 1f; offsetX = 0f; offsetY = 0f }
                                )
                            })
                        Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            shape = RoundedCornerShape(8.dp), color = Color(0x73000000)) {
                            Text(if (zoomScale > 1.01f) "👆 双击还原" else "👆 点击翻面 · 双指缩放",
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp, color = Color.White)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize().clickable { vm.flip() }
                        .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.secondaryContainer))),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Text(item.phylum, fontSize = 15.sp, color = cs.onSurfaceVariant)
                        if (item.phylumLatin.isNotEmpty())
                            Text(item.phylumLatin, fontSize = 12.sp,
                                color = cs.onSurfaceVariant.copy(alpha = 0.5f),
                                fontStyle = FontStyle.Italic)
                        Spacer(Modifier.height(8.dp))
                        Text(item.genus, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                        if (item.genusLatin.isNotEmpty())
                            Text(item.genusLatin, fontSize = 16.sp,
                                color = cs.primary.copy(alpha = 0.5f), fontStyle = FontStyle.Italic)
                        Spacer(Modifier.height(12.dp))
                        if (known) Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFC8E6C9)) {
                            Text("✓ 已掌握", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
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
                Text("↩ 撤销上一张", fontSize = 14.sp, color = Color(0xFF1565C0)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ vm.mark(true) }, Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)) {
                Text("✓ 认识", Modifier.padding(vertical = 8.dp), fontSize = 16.sp) }
            Button({ vm.mark(false) }, Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(12.dp)) {
                Text("✗ 不认识", Modifier.padding(vertical = 8.dp), fontSize = 16.sp) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ═══════════ 错题 Tab ═══════════

@Composable
private fun ErrorBookTab(vm: TrainViewModel, onZoom: (List<Pair<AlgaeItem, Int>>, Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val entries = remember(vm.errorBookVersion) { vm.errorBook() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("📋 错题集", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                if (entries.isNotEmpty()) TextButton(onClick = { vm.clearErrors() }) {
                    Text("清空", color = cs.error)
                }
            }
            Text("错误的图片会上传到你的错题集里，方便你反复查看。",
                fontSize = 12.sp, color = cs.outline)
            Spacer(Modifier.height(8.dp))

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Text("没有错题！", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(entries) { idx, (item, count) ->
                        Card(Modifier.fillMaxWidth().clickable { onZoom(entries, idx) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data("file:///android_asset/${vm.displayPrefix}${item.file}")
                                        .crossfade(true).size(160).build(),
                                    contentDescription = item.genus,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.genus, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                        color = cs.onSurface)
                                    Text(item.phylum, fontSize = 13.sp, color = cs.outline)
                                    Text("错误 $count 次", fontSize = 12.sp, color = cs.error)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ═══════════ 关于 Tab ═══════════

@Composable
private fun AboutTab(vm: TrainViewModel) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        AboutCard("📖 玩法说明", cs) {
            Text("切换「🌿 浮游植物」和「🦠 浮游动物」训练。看图片判断是否认识。不认识的项目自动加入错题集。",
                lineHeight = 24.sp, color = cs.onSurface)
            Spacer(Modifier.height(12.dp))
            for ((n, t) in listOf("1" to "顶端选择浮游植物或浮游动物", "2" to "看图片判断是否认识",
                "3" to "单击卡牌翻面查看名称", "4" to "认识点✓，不认识点✗",
                "5" to "点错可点「↩撤销」返回重标", "6" to "不认识自动加入错题集",
                "7" to "退出后进度自动保存")) {
                Row(Modifier.padding(vertical = 4.dp)) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(cs.primaryContainer),
                        contentAlignment = Alignment.Center) {
                        Text(n, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    Text(t, Modifier.weight(1f), lineHeight = 22.sp, color = cs.onSurface)
                }
            }
        }

        AboutCard("📱 版本信息", cs) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("版本", color = cs.outline); Text(VERSION, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("系统", color = cs.outline); Text("Android 8.0+")
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("浮游植物", color = cs.outline); Text("${vm.allAlgaeItems.size} 种", fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("浮游动物", color = cs.outline); Text("${vm.allZooItems.size} 种", fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("技术栈", color = cs.outline); Text("Kotlin + Compose + Coil")
            }
        }

        AboutCard("👥 作者信息", cs) {
            val authors = listOf("喝茶喵", "蛋蛋", "进宝", "斯卡蒂")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                authors.forEach { name ->
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        color = cs.primaryContainer) {
                        Text(name, Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = cs.onPrimaryContainer)
                    }
                }
            }
        }

        // Copyright
        AboutCard("📸 图片来源", cs) {
            Text("图片素材来源于公开出版物及实地采样：", lineHeight = 22.sp, color = cs.onSurface)
            Spacer(Modifier.height(8.dp))
            Text("📗 《中国内陆水域常见藻类图谱》", fontWeight = FontWeight.Bold, color = cs.onSurface)
            Text("📘 《澳门常见淡水藻类图谱》", fontWeight = FontWeight.Bold, color = cs.onSurface)
            Text("📘 《中国流域常见水生生物图集》", fontWeight = FontWeight.Bold, color = cs.onSurface)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("⚠️ 版权声明", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cs.error)
            Text("• 图片版权归原作者及出版社所有", lineHeight = 22.sp, color = cs.onSurface)
            Text("• 仅用于学术教育目的，完全免费开源", lineHeight = 22.sp, color = cs.onSurface)
            Text("• 禁止商业用途", lineHeight = 22.sp, color = cs.onSurface)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AboutCard(title: String, cs: androidx.compose.material3.ColorScheme,
                      content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)) {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cs.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ═══════════ 全屏缩放查看（按钮翻页） ═══════════

@Composable
private fun FullscreenZoom(
    entries: List<Pair<AlgaeItem, Int>>, initialIdx: Int,
    prefix: String, onClose: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(initialIdx) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val canPrev = currentPage > 0
    val canNext = currentPage < entries.size - 1

    // Reset zoom on page change
    LaunchedEffect(currentPage) {
        zoomScale = 1f; offsetX = 0f; offsetY = 0f
    }

    val (item, _) = entries[currentPage]
    val assetPath = "${prefix}${item.file}"

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f))) {
        // ── Image with zoom / tap gestures ──
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("file:///android_asset/$assetPath").crossfade(true).build(),
            contentDescription = item.genus,
            modifier = Modifier.fillMaxSize()
                .pointerInput(currentPage) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val s = (zoomScale * zoom).coerceIn(1f, 6f)
                        zoomScale = s
                        if (s > 1.01f) { offsetX += pan.x; offsetY += pan.y }
                        else { offsetX = 0f; offsetY = 0f }
                    }
                }
                .pointerInput(currentPage) {
                    detectTapGestures(
                        onTap = { onClose() },
                        onDoubleTap = { zoomScale = 1f; offsetX = 0f; offsetY = 0f }
                    )
                }
                .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale,
                    translationX = offsetX, translationY = offsetY),
            contentScale = ContentScale.Fit)

        // ── Top bar: ← 返回 + page indicator + species name ──
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth()
            .background(Color(0x99000000)).padding(top = 48.dp, bottom = 12.dp)
            .clickable { onClose() },
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回",
                tint = Color.White, modifier = Modifier.padding(start = 16.dp).size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("${currentPage + 1}/${entries.size}",
                color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text(item.genus,
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.weight(1f))
            TextButton(onClick = onClose,
                modifier = Modifier.padding(end = 8.dp)) {
                Text("退出", color = Color(0xFF81C784), fontSize = 14.sp)
            }
        }

        // ── Bottom bar: prev/next + species info ──
        Surface(Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), color = Color(0x99000000)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                    // Prev
                    IconButton(onClick = { if (canPrev) currentPage-- },
                        enabled = canPrev,
                        modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一张",
                            tint = if (canPrev) Color.White else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(24.dp))
                    }
                    // Species info
                    Column(Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.genus, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${item.phylum}  ·  ${item.genusLatin}",
                            color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    // Next
                    IconButton(onClick = { if (canNext) currentPage++ },
                        enabled = canNext,
                        modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一张",
                            tint = if (canNext) Color.White else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(24.dp))
                    }
                }
                if (zoomScale <= 1.01f) {
                    Text("👆 点击退出 · 双指缩放 · 双击还原",
                        Modifier.padding(bottom = 6.dp),
                        fontSize = 10.sp, color = Color(0xFF81C784))
                }
            }
        }
    }
}
