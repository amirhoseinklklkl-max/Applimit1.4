package ir.amir.applimiter.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.applimiter.ads.AdiveryBanner
import ir.amir.applimiter.data.AppEntry
import ir.amir.applimiter.data.InstalledAppsRepository
import ir.amir.applimiter.data.LimitConfig
import ir.amir.applimiter.data.LimitStore
import ir.amir.applimiter.usage.UsageLedger
import ir.amir.applimiter.usage.asHumanDuration
import ir.amir.applimiter.usage.minutesAsHuman
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun AppListScreen(refreshKey: Int) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var limits by remember { mutableStateOf<Map<String, LimitConfig>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<AppEntry?>(null) }

    LaunchedEffect(refreshKey) {
        val loaded = withContext(Dispatchers.IO) { InstalledAppsRepository.load(context) }
        apps = loaded
        usage = UsageLedger.all(context)
        limits = LimitStore.all(context).associateBy { it.packageName }
        loading = false
    }

    // اعداد مصرف را زنده نگه می‌دارد تا کاربر ببیند تایمر کار می‌کند
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            usage = UsageLedger.all(context)
        }
    }

    val filtered = apps.filter {
        (query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true)) &&
            when (tab) {
                1 -> it.isGame
                2 -> !it.isGame
                3 -> limits.containsKey(it.packageName)
                else -> true
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("جستجوی برنامه یا بازی") }
        )

        TabRow(selectedTabIndex = tab) {
            listOf("همه", "بازی‌ها", "برنامه‌ها", "محدودشده").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }

        if (loading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        config = limits[app.packageName],
                        usedToday = usage[app.packageName] ?: 0L,
                        onClick = { selected = app }
                    )
                    HorizontalDivider()
                }
            }
        }

        // بنر تبلیغاتی تپسل، چسبیده به پایین صفحه
        AdiveryBanner()
    }

    val current = selected
    if (current != null) {
        LimitDialog(
            app = current,
            current = limits[current.packageName],
            usedTodayMillis = usage[current.packageName] ?: 0L,
            onDismiss = { selected = null },
            onSave = { minutes, askDaily ->
                val existing = limits[current.packageName]
                LimitStore.save(
                    context,
                    LimitConfig(
                        packageName = current.packageName,
                        limitMinutes = minutes,
                        askDaily = askDaily,
                        confirmedDay = if (askDaily) "" else (existing?.confirmedDay ?: ""),
                        todayLimitMinutes = if (askDaily) 0 else minutes
                    )
                )
                limits = LimitStore.all(context).associateBy { it.packageName }
                selected = null
            },
            onRemove = {
                LimitStore.remove(context, current.packageName)
                limits = LimitStore.all(context).associateBy { it.packageName }
                selected = null
            },
            onResetUsage = {
                UsageLedger.reset(context, current.packageName)
                usage = UsageLedger.all(context)
            }
        )
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    config: LimitConfig?,
    usedToday: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val bitmap = app.icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
        } else {
            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(44.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(app.label, fontWeight = FontWeight.SemiBold)
                if (app.isGame) {
                    Text(
                        "بازی",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            val limitText = when {
                config == null -> "بدون محدودیت"
                config.askDaily && config.confirmedDay != LimitStore.todayKey() ->
                    "هر روز پرسیده می‌شود"
                else -> "سهمیه: ${(LimitStore.effectiveMinutes(config) ?: 0).minutesAsHuman()}"
            }
            Text(
                "امروز ${usedToday.asHumanDuration()} • $limitText",
                style = MaterialTheme.typography.bodySmall
            )
            val minutes = config?.let { LimitStore.effectiveMinutes(it) }
            if (minutes != null && minutes > 0) {
                val ratio = (usedToday.toFloat() / (minutes * 60_000f)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}
