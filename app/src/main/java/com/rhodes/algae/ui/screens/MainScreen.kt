package com.rhodes.algae.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rhodes.algae.data.AlgaeItem
import com.rhodes.algae.ui.theme.ThemeState
import com.rhodes.algae.viewmodel.TrainViewModel
import java.time.LocalDate
import java.time.YearMonth

private const val VERSION = "V0.5.0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: TrainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    // 首次启动未选书时自动进入图谱书架
    var showShelf by remember { mutableStateOf(!vm.bookSelected) }
    val cs = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize()) {
        // 图谱书架打开时拦截系统返回键
        if (showShelf) BackHandler(enabled = true) { showShelf = false }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(when (tab) { 0 -> "📖 识浮游"; 1 -> "📅 打卡"; else -> "ℹ️ 关于" },
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
                        icon = { Icon(Icons.Filled.CheckCircle, "打卡") }, label = { Text("打卡") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                        icon = { Icon(Icons.Filled.Info, "关于") }, label = { Text("关于") })
                }
            },
            containerColor = cs.background
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    0 -> TrainTab(vm)
                    1 -> CheckInTab(vm, onOpenShelf = { showShelf = true })
                    2 -> AboutTab(vm)
                }
            }
        }

        // 图谱书架 overlay
        if (showShelf) {
            BookShelfScreen(vm, canClose = vm.bookSelected, onClose = { showShelf = false })
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

    if (vm.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f)) {
            val item = vm.currentItem
            if (item != null) FlashcardView(vm, item)
            else CompleteView(vm)
        }
    }
}

// ═══════════ 打卡 Tab ═══════════

@Composable
private fun CheckInTab(vm: TrainViewModel, onOpenShelf: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var showRestart by remember { mutableStateOf(false) }
    var showQuotaDialog by remember { mutableStateOf(false) }
    var quotaInput by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 我的图谱书 + 总进度（点击换书）
        item {
            Card(Modifier.fillMaxWidth().clickable(onClick = onOpenShelf),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(if (vm.currentMode == "algae") "🌿" else "🦠", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("📚 我的图谱书", fontSize = 13.sp, color = cs.outline)
                        Spacer(Modifier.height(2.dp))
                        Text(vm.bookTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                        Text("${vm.currentPhylumCount} 门 · ${vm.allItems.size} 图 · 点击卡片可切换图谱书",
                            fontSize = 12.sp, color = cs.outline)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (vm.allItems.isEmpty()) 0f else vm.knownCount.toFloat() / vm.allItems.size },
                            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = cs.primary, trackColor = cs.surface)
                        Spacer(Modifier.height(4.dp))
                        Text("已掌握 ${vm.knownCount}/${vm.allItems.size} · 未掌握 ${vm.allItems.size - vm.knownCount}",
                            fontSize = 12.sp, color = cs.outline)
                    }
                    Spacer(Modifier.width(8.dp))
                    // 显眼的切换按钮
                    Surface(shape = RoundedCornerShape(10.dp), color = cs.primary) {
                        Text("切换", Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cs.onPrimary)
                    }
                }
            }
        }

        // 今日任务
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(cs.surfaceVariant).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("📅 今日任务", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text("新卡 ${vm.newDone}/${vm.newTotal} · 复习 ${vm.reviewDone}/${vm.reviewTotal} · 连续 ${vm.streakDays()} 天 🔥",
                            fontSize = 12.sp, color = cs.onSurfaceVariant)
                    }
                    if (vm.isCheckedIn()) Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFC8E6C9)) {
                        Text("✓ 已打卡", Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
                }
                Spacer(Modifier.height(10.dp))
                Text("新卡", fontSize = 11.sp, color = cs.outline)
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { if (vm.newTotal == 0) 0f else vm.newDone.toFloat() / vm.newTotal },
                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = cs.primary, trackColor = cs.surface)
                Spacer(Modifier.height(8.dp))
                Text("复习", fontSize = 11.sp, color = cs.outline)
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { if (vm.reviewTotal == 0) 0f else vm.reviewDone.toFloat() / vm.reviewTotal },
                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = cs.secondary, trackColor = cs.surface)
            }
        }

        // 打卡月历
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(cs.surfaceVariant).padding(12.dp)) {
                Text("🗓 打卡月历", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                Spacer(Modifier.height(8.dp))
                MonthCalendar(vm)
            }
        }

        // 打卡设置
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(cs.surfaceVariant).padding(12.dp)) {
                Text("⚙️ 打卡设置", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                Spacer(Modifier.height(10.dp))
                Text("每日新卡量", fontSize = 13.sp, color = cs.outline)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrainViewModel.QUOTA_OPTIONS.forEach { q ->
                        FilterChip(selected = vm.dailyQuota == q, onClick = { vm.setQuota(q) },
                            label = { Text("$q", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                    }
                    FilterChip(
                        selected = vm.dailyQuota !in TrainViewModel.QUOTA_OPTIONS,
                        onClick = { quotaInput = vm.dailyQuota.toString(); showQuotaDialog = true },
                        label = { Text("自定义", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                }
                // 始终显示当前实际新卡量（档位/自定义都可见）
                Spacer(Modifier.height(4.dp))
                Text("当前 ${vm.dailyQuota} 张/天" +
                    if (vm.dailyQuota !in TrainViewModel.QUOTA_OPTIONS) "（自定义）" else "",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                Spacer(Modifier.height(10.dp))
                Text("新学 : 复习", fontSize = 13.sp, color = cs.outline)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrainViewModel.RATIO_OPTIONS.forEach { r ->
                        FilterChip(selected = vm.reviewRatio == r, onClick = { vm.setRatio(r) },
                            label = { Text("1:$r", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                    }
                }
                // 始终显示当前复习比例（与新卡量提示一致）
                Spacer(Modifier.height(4.dp))
                Text("当前 新学:复习 = 1:${vm.reviewRatio}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("重置：清空两本图谱的学习进度", Modifier.weight(1f),
                        fontSize = 12.sp, color = cs.outline)
                    TextButton(onClick = { showRestart = true }) { Text("重置", color = cs.error) }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showRestart) AlertDialog(
        onDismissRequest = { showRestart = false },
        title = { Text("确认重新开始") },
        text = { Text("两本图谱的学习进度和复习计划将丢失（打卡记录保留），确定？") },
        confirmButton = { TextButton({ vm.restart(); showRestart = false }) { Text("确定") } },
        dismissButton = { TextButton({ showRestart = false }) { Text("取消") } })

    if (showQuotaDialog) AlertDialog(
        onDismissRequest = { showQuotaDialog = false },
        title = { Text("自定义每日新卡量") },
        text = {
            OutlinedTextField(
                value = quotaInput,
                onValueChange = { quotaInput = it.filter(Char::isDigit).take(3) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("张/天（${TrainViewModel.QUOTA_MIN}-${TrainViewModel.QUOTA_MAX}）") })
        },
        confirmButton = { TextButton({
            quotaInput.toIntOrNull()?.let { vm.setQuota(it) }
            showQuotaDialog = false
        }) { Text("确定") } },
        dismissButton = { TextButton({ showQuotaDialog = false }) { Text("取消") } })
}

// ═══════════ 打卡月历 ═══════════

@Composable
private fun MonthCalendar(vm: TrainViewModel) {
    val cs = MaterialTheme.colorScheme
    var month by remember { mutableStateOf(YearMonth.now()) }
    val checkins = remember(vm.checkinVersion) { vm.checkedInDays().toSet() }
    val today = LocalDate.now()

    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上月", tint = cs.outline) }
            Text("${month.year} 年 ${month.monthValue} 月", Modifier.weight(1f),
                textAlign = TextAlign.Center, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = cs.onSurface)
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下月", tint = cs.outline) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Text(w, Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 11.sp, color = cs.outline)
            }
        }
        Spacer(Modifier.height(4.dp))
        val firstDow = (month.atDay(1).dayOfWeek.value + 6) % 7 // 周一 = 0（value: 周一1..周日7）
        val days = month.lengthOfMonth()
        val totalCells = ((firstDow + days + 6) / 7) * 7
        for (row in 0 until totalCells step 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = row + col - firstDow + 1
                    val valid = day in 1..days
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                        contentAlignment = Alignment.Center) {
                        if (valid) {
                            val date = month.atDay(day)
                            val checked = date.toEpochDay() in checkins
                            val isToday = date == today
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                .background(if (checked) cs.primary else Color.Transparent)
                                .then(if (isToday)
                                    Modifier.border(1.dp, cs.primary, RoundedCornerShape(8.dp))
                                else Modifier),
                                contentAlignment = Alignment.Center) {
                                Text("$day", fontSize = 12.sp,
                                    color = when {
                                        checked -> cs.onPrimary
                                        date.isAfter(today) -> cs.outline.copy(alpha = 0.4f)
                                        else -> cs.onSurface
                                    },
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompleteView(vm: TrainViewModel) {
    val cs = MaterialTheme.colorScheme
    var noMore by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("🎉", fontSize = 48.sp)
        Text("今日任务完成！", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cs.primary)
        Spacer(Modifier.height(4.dp))
        Text("已掌握 ${vm.knownCount}/${vm.allItems.size} · 连续打卡 ${vm.streakDays()} 天",
            color = cs.outline)
        Spacer(Modifier.height(8.dp))
        if (vm.isCheckedIn()) Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFC8E6C9)) {
            Text("✓ 今日已打卡", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
        Spacer(Modifier.height(20.dp))
        // 主按钮：再学一组（从剩余新卡加练，不影响打卡）
        Button(onClick = {
            if (vm.addMoreCards() > 0) noMore = false
            else noMore = true
        }, enabled = !noMore,
            colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
            shape = RoundedCornerShape(12.dp)) {
            Text(if (noMore) "图谱已全部学完 🎉" else "再学一组",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp), fontSize = 16.sp)
        }
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

// ═══════════ 关于 Tab ═══════════

@Composable
private fun AboutTab(vm: TrainViewModel) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        AboutCard("📖 玩法说明", cs) {
            Text("在「打卡」页的「我的图谱书」卡片选择图谱书（浮游植物 / 浮游动物），每天完成新卡和复习任务，按记忆曲线安排复习。",
                lineHeight = 24.sp, color = cs.onSurface)
            Spacer(Modifier.height(12.dp))
            for ((n, t) in listOf("1" to "在打卡页点「我的图谱书」卡片选择/切换图谱书",
                "2" to "看图片判断是否认识", "3" to "单击卡牌翻面查看名称",
                "4" to "认识点✓，不认识点✗（不认识会归零重学）",
                "5" to "点错可点「↩撤销」返回重标",
                "6" to "按记忆曲线（1/2/4/7/15/30 天）安排复习",
                "7" to "每日新卡学完自动打卡，可翻看打卡月历",
                "8" to "学完可点「再学一组」继续加练（不影响打卡）")) {
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
                Text("浮游植物", color = cs.outline); Text("${vm.allAlgaeItems.size} 图", fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("浮游动物", color = cs.outline); Text("${vm.allZooItems.size} 图", fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("技术栈", color = cs.outline); Text("Kotlin + Compose + Coil")
            }
        }

        AboutCard("👥 作者信息", cs) {
            val authors = listOf("喝茶喵", "蛋蛋", "进宝")
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
