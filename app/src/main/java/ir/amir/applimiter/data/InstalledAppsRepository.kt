package ir.amir.applimiter.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.core.graphics.drawable.toBitmap

/** همه‌ی برنامه‌ها و بازی‌های نصب‌شده‌ای که آیکن لانچر دارند را برمی‌گرداند. */
object InstalledAppsRepository {

    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(launcherIntent, 0)
        val map = LinkedHashMap<String, AppEntry>()

        for (info in resolved) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || map.containsKey(pkg)) continue
            val appInfo: ApplicationInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (e: Exception) {
                continue
            }
            val isGame = appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            val icon = try {
                info.loadIcon(pm)?.toBitmap(96, 96)
            } catch (e: Exception) {
                null
            }
            map[pkg] = AppEntry(
                packageName = pkg,
                label = info.loadLabel(pm).toString(),
                isGame = isGame,
                icon = icon
            )
        }

        return map.values.sortedWith(
            compareByDescending<AppEntry> { it.isGame }.thenBy { it.label.lowercase() }
        )
    }

    fun labelOf(context: Context, pkg: String): String {
        val pm = context.packageManager
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    fun launch(context: Context, pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
