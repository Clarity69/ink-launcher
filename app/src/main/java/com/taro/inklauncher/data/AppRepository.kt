package com.taro.inklauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val isSystem: Boolean,
)

/**
 * Daftar aplikasi dari LauncherApps, diperbarui otomatis lewat LauncherApps.Callback.
 */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private val _packageChanged = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val packageChanged: SharedFlow<String> = _packageChanged

    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private var reloadJob: Job? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = changed(packageName)
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = changed(packageName)
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            packageNames?.forEach { changed(it) }
        }
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            packageNames?.forEach { changed(it) }
        }
    }

    fun start() {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    private fun changed(pkg: String?) {
        if (pkg != null) _packageChanged.tryEmit(pkg)
        reload(debounce = true)
    }

    fun reload(debounce: Boolean = false) {
        reloadJob?.cancel()
        reloadJob = scope.launch {
            if (debounce) delay(300)
            val list = withContext(Dispatchers.IO) { load() }
            _apps.value = list
            _loaded.value = true
        }
    }

    private fun load(): List<AppEntry> {
        val result = ArrayList<AppEntry>()
        val newInfos = HashMap<String, LauncherActivityInfo>()
        val profiles = runCatching { userManager.userProfiles }.getOrDefault(listOf(Process.myUserHandle()))
        for (user in profiles) {
            val list = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            for (info in list) {
                if (info.componentName.packageName == context.packageName) continue
                val key = keyOf(info.componentName, user)
                newInfos[key] = info
                val flags = info.applicationInfo.flags
                result += AppEntry(
                    key = key,
                    label = info.label?.toString()?.trim().orEmpty().ifEmpty { info.componentName.packageName },
                    packageName = info.componentName.packageName,
                    component = info.componentName,
                    user = user,
                    isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                        (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0,
                )
            }
        }
        infos.clear()
        infos.putAll(newInfos)
        val collator = Collator.getInstance()
        return result.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun keyOf(component: ComponentName, user: UserHandle): String =
        component.flattenToShortString() + "#" + userManager.getSerialNumberForUser(user)

    fun activityInfo(entry: AppEntry): LauncherActivityInfo? =
        infos[entry.key] ?: runCatching {
            launcherApps.resolveActivity(Intent(Intent.ACTION_MAIN).setComponent(entry.component), entry.user)
        }.getOrNull()

    fun launch(entry: AppEntry, bounds: Rect? = null) {
        runCatching { launcherApps.startMainActivity(entry.component, entry.user, bounds, null) }
            .onFailure {
                runCatching {
                    context.packageManager.getLaunchIntentForPackage(entry.packageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ?.let { i -> context.startActivity(i) }
                }
            }
    }

    fun openAppInfo(entry: AppEntry) {
        runCatching { launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null) }
    }

    /** App shortcuts; hanya tersedia bila InkLauncher adalah launcher default. */
    fun shortcuts(entry: AppEntry): List<ShortcutInfo> {
        return try {
            if (!launcherApps.hasShortcutHostPermission()) return emptyList()
            val q = LauncherApps.ShortcutQuery()
                .setPackage(entry.packageName)
                .setActivity(entry.component)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            (launcherApps.getShortcuts(q, entry.user) ?: emptyList())
                .filter { it.isEnabled }
                .sortedBy { it.rank }
                .take(5)
        } catch (t: Throwable) {
            emptyList()
        }
    }

    fun startShortcut(shortcut: ShortcutInfo) {
        runCatching { launcherApps.startShortcut(shortcut, null, null) }
    }
}
