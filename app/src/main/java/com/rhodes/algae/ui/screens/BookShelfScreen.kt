package com.rhodes.algae.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhodes.algae.data.AlgaeItem
import com.rhodes.algae.viewmodel.TrainViewModel

/**
 * 图谱书架：像选词书一样选择「浮游植物图谱」或「浮游动物图谱」。
 * 全屏覆盖层，由 MainScreen 控制显隐。
 */
@Composable
fun BookShelfScreen(vm: TrainViewModel, canClose: Boolean, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.background)) {
        Column(Modifier.fillMaxSize()) {
            // Top bar
            Row(Modifier.fillMaxWidth().background(cs.primary)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically) {
                if (canClose) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = cs.onPrimary)
                    }
                } else {
                    Spacer(Modifier.width(16.dp))
                }
                Text("📚 我的图谱书", Modifier.weight(1f),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = cs.onPrimary)
                Spacer(Modifier.width(16.dp))
            }
            // 说明：让用户明白可以选不同的书
            Text("选择一本图谱书开始学习，随时可以切换另一本（两本进度独立保存）",
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontSize = 13.sp, color = cs.outline)
            LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    BookCard(vm, "algae", "🌿", "浮游植物图谱", vm.allAlgaeItems, onClose)
                }
                item {
                    BookCard(vm, "zooplankton", "🦠", "浮游动物图谱", vm.allZooItems, onClose)
                }
            }
        }
    }
}

@Composable
private fun BookCard(vm: TrainViewModel, mode: String, emoji: String, title: String,
                     items: List<AlgaeItem>, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val selected = vm.currentMode == mode
    val phylumCount = vm.phylumCountFor(mode)
    val known = items.count { vm.isKnownFor(mode, it.id) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = items.isNotEmpty()) { vm.switchMode(mode); onClose() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) cs.primaryContainer else cs.surfaceVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(cs.primary),
                contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 28.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                }
                Spacer(Modifier.height(4.dp))
                if (items.isEmpty()) {
                    Text("加载中…", fontSize = 13.sp, color = cs.outline)
                } else {
                    Text("$phylumCount 门 · ${items.size} 图", fontSize = 13.sp, color = cs.outline)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (items.isEmpty()) 0f else known.toFloat() / items.size },
                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                    color = cs.primary, trackColor = cs.surface)
                Spacer(Modifier.height(4.dp))
                Text("已掌握 $known/${items.size}", fontSize = 12.sp, color = cs.outline)
            }
            Spacer(Modifier.width(12.dp))
            // 明确的操作按钮：学习中 / 选择此书
            if (selected) {
                Surface(shape = RoundedCornerShape(12.dp), color = cs.primary) {
                    Text("✓ 学习中", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cs.onPrimary)
                }
            } else {
                Surface(shape = RoundedCornerShape(12.dp), color = cs.secondaryContainer) {
                    Text("选择此书", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cs.onSecondaryContainer)
                }
            }
        }
    }
}
