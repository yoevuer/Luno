package hunoia.luno.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import hunoia.luno.R
import hunoia.luno.config.model.Action
import hunoia.luno.action.api.ActionHandlerContext
import hunoia.luno.action.api.ActionRegistry
import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.GestureButtonActionSettingsOverride
import hunoia.luno.config.model.ActionSettings
import hunoia.luno.config.model.effectiveFor
import hunoia.luno.bridge.feedback.showToast
import hunoia.luno.bridge.feedback.showToastLong
import hunoia.luno.bridge.feedback.showVersionTooLowToast as showVersionTooLowToastUtil
import java.util.LinkedHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SideGestureServiceProxyActionCoordinator(
    private val host: SideGestureService,
    private val scopeProvider: () -> CoroutineScope,
) {
    private companion object {
        const val KEEP_SCREEN_ON_WAKE_LOCK_TIMEOUT_MS = 2 * 60 * 1000L
    }

    private var prevPackageName: String? = null
    private var currPackageName: String? = null
    private val launchablePackageCache = object : LinkedHashMap<String, Boolean>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > 256
    }
    private val activityExistsCache = object : LinkedHashMap<String, Boolean>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > 512
    }

    private var wakeLock: PowerManager.WakeLock? = null

    fun onRelease() {
        safeReleaseWakeLock()
        launchablePackageCache.clear()
        activityExistsCache.clear()
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                val className = event.className?.toString()

                isActivity(packageName, className)
                val prevAppExcludePkgNames = host.actionSettings?.previousApp?.packageNames ?: emptyList()
                if (packageName !in prevAppExcludePkgNames &&
                    hasLaunchIntent(packageName) &&
                    currPackageName != packageName
                ) {
                    prevPackageName = currPackageName
                    currPackageName = packageName
                    if (prevPackageName == null) {
                        prevPackageName = currPackageName
                    }
                }
            }
            else -> Unit
        }
    }

    fun onAction(action: Action, sourceButton: GestureButton?, sourceOverride: GestureButtonActionSettingsOverride? = sourceButton?.actionSettingsOverride) {
        val scope = scopeProvider()
        scope.launch(Dispatchers.Main.immediate) {
            ActionRegistry.execute(action, buildActionHandlerContext(sourceButton, sourceOverride))
        }
    }

    private fun buildActionHandlerContext(sourceButton: GestureButton?, sourceOverride: GestureButtonActionSettingsOverride?): ActionHandlerContext {
        return ActionHandlerContext(
            accessibilityService = host,
            appContext = host.applicationContext,
            scope = scopeProvider(),
            actionSettings = (host.actionSettings ?: ActionSettings()).effectiveFor(sourceOverride),
            advancedSettings = (host.advancedSettings ?: hunoia.luno.config.model.AdvancedSettings()).effectiveFor(sourceOverride),
            gestureSettings = (host.gestureSettings ?: hunoia.luno.config.model.GestureSettings()).effectiveFor(sourceOverride),
            showToast = { showToast(it) },
            showLongToast = { showToastLong(it) },
            currentPackageName = { currPackageName },
            nowInLauncher = { host.nowInLauncher() },
            requestEnableFrozenPackage = { packageName, onResult ->
                host.requestEnableFrozenPackage(packageName, onResult)
            },
            toggleQuickAppLauncher = { host.quickAppLauncherOverlay.toggle() },
            showPointer = { continuousModeOverride -> host.showPointerOverlay(continuousModeOverride) },
            showVolumeScrub = { host.showVolumeScrubOverlay() },
            hideGestureButton = { delayMs ->
                if (sourceButton != null) {
                    host.hideGestureButtonTemporarily(sourceButton, delayMs)
                }
            },
            toggleKeepScreenOn = {
                if (wakeLock != null) {
                    safeReleaseWakeLock()
                    showToast(R.string.disable_keep_screen_on)
                } else {
                    val pm = host.getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "gulugulu:KeepScreenOn")
                    wakeLock?.setReferenceCounted(false)
                    wakeLock?.acquire(KEEP_SCREEN_ON_WAKE_LOCK_TIMEOUT_MS)
                    showToast(R.string.enable_keep_screen_on)
                }
            },
            showVersionTooLowToast = { resId ->
                showVersionTooLowToastUtil(host, resId)
            },
            previousApp = {
                previousApp()
            },
        )
    }

    private suspend fun previousApp() {
        val prevPkgName = prevPackageName
        val curPkgName = currPackageName
        if (prevPkgName.isNullOrEmpty() || curPkgName.isNullOrEmpty()) {
            return
        }
        if (currPackageNameError()) {
            queryLaunchIntentAndStart(curPkgName)
            return
        }
        if (prevPkgName == curPkgName) return
        if (queryLaunchIntentAndStart(prevPkgName)) {
            prevPackageName = curPkgName
            currPackageName = prevPkgName
        }
    }

    private suspend fun queryLaunchIntentAndStart(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) {
            return false
        }
        val intent = withContext(Dispatchers.IO) {
            host.packageManager.getLaunchIntentForPackage(packageName)
        } ?: return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            host.startActivity(intent)
            true
        } catch (ignored: Exception) {
            false
        }
    }

    private fun currPackageNameError(): Boolean {
        val pkgName = host.rootInActiveWindow?.packageName?.toString()
        return pkgName != currPackageName
    }

    private fun hasLaunchIntent(packageName: String?): Boolean {
        val key = packageName ?: return false
        launchablePackageCache[key]?.let { return it }
        val result = host.packageManager.getLaunchIntentForPackage(key) != null
        launchablePackageCache[key] = result
        return result
    }

    private fun isActivity(packageName: String?, className: String?): Boolean {
        packageName ?: return false
        className ?: return false
        val key = "$packageName/$className"
        activityExistsCache[key]?.let { return it }
        return try {
            val component = ComponentName(packageName, className)
            host.packageManager.getActivityInfo(component, 0)
            cacheActivityExists(key, true)
            true
        } catch (e: Exception) {
            cacheActivityExists(key, false)
            false
        }
    }

    private fun cacheActivityExists(key: String, value: Boolean) {
        activityExistsCache[key] = value
    }

    private fun safeReleaseWakeLock() {
        val lock = wakeLock
        wakeLock = null
        if (lock?.isHeld == true) {
            try {
                lock.release()
            } catch (ignored: RuntimeException) {
                // Ignore defensively.
            }
        }
    }

}
