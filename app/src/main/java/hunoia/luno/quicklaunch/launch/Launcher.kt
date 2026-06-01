package hunoia.luno.quicklaunch.launch

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.view.WindowManager
import hunoia.luno.R
import hunoia.luno.quicklaunch.model.AppInfo
import hunoia.luno.quicklaunch.model.LauncherInfo
import hunoia.luno.config.model.OpenAppOrUrlData
import hunoia.luno.bridge.feedback.showToast
import kotlin.math.roundToInt

object Launcher {

    fun launchApp(
        context: Context,
        packageName: String,
        className: String,
        miniWindow: Boolean,
        miniWindowHorizontalBias: Float = DefaultMiniWindowHorizontalBias,
        miniWindowVerticalBias: Float = DefaultMiniWindowVerticalBias,
        miniWindowVerticalOffsetFraction: Float = DefaultMiniWindowVerticalOffsetFraction,
        miniWindowWidthFraction: Float = DefaultMiniWindowWidthFraction,
        miniWindowHeightFraction: Float = DefaultMiniWindowHeightFraction,
        overrideBounds: Boolean = false,
    ): Boolean {
        if (miniWindow) {
            return launchAppInPopup(
                context, packageName, className,
                miniWindowHorizontalBias, miniWindowVerticalBias,
                miniWindowVerticalOffsetFraction,
                miniWindowWidthFraction, miniWindowHeightFraction,
                overrideBounds = overrideBounds,
            )
        }
        return try {
            val intent = Intent().apply {
                setClassName(packageName, className)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.resolveActivity(intent, 0) == null) {
                val fallback = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
                context.startActivity(fallback)
                return true
            }
            context.startActivity(intent)
            true
        } catch (ignored: Exception) {
            false
        }
    }

    fun launchAppActivity(context: Context, packageName: String, className: String): Boolean {
        return try {
            val component = ComponentName.createRelative(packageName, className)
            val intent = Intent().apply {
                setComponent(component)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.resolveActivity(intent, 0) == null) return false
            context.startActivity(intent)
            true
        } catch (ignored: Exception) {
            false
        }
    }

    fun launchAppInfo(
        context: Context,
        appInfo: AppInfo,
        miniWindow: Boolean,
        miniWindowHorizontalBias: Float = DefaultMiniWindowHorizontalBias,
        miniWindowVerticalBias: Float = DefaultMiniWindowVerticalBias,
        miniWindowVerticalOffsetFraction: Float = DefaultMiniWindowVerticalOffsetFraction,
        miniWindowWidthFraction: Float = DefaultMiniWindowWidthFraction,
        miniWindowHeightFraction: Float = DefaultMiniWindowHeightFraction,
        overrideBounds: Boolean = false,
    ): Boolean {
        return launchApp(
            context, appInfo.packageName, appInfo.className, miniWindow,
            miniWindowHorizontalBias, miniWindowVerticalBias,
            miniWindowVerticalOffsetFraction,
            miniWindowWidthFraction, miniWindowHeightFraction,
            overrideBounds = overrideBounds,
        )
    }

    fun launchShortcutInfo(context: Context, shortcutInfo: LauncherInfo.ShortcutInfo): Boolean {
        return try {
            val intents = shortcutInfo.intents.map {
                Intent.parseUri(it, Intent.URI_INTENT_SCHEME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }.toTypedArray()
            context.startActivities(intents)
            true
        } catch (ignored: Exception) {
            showToast(context.getString(R.string.launch_shortcut_info_failed, shortcutInfo.label))
            false
        }
    }

    fun launchOpenAppOrUrl(context: Context, data: OpenAppOrUrlData): Boolean {
        return when (data.type) {
            OpenAppOrUrlData.TYPE_ACTIVITY -> {
                if (data.packageName.isBlank() || data.activityClassName.isBlank()) {
                    showToast(context.getString(R.string.launch_failed))
                    false
                } else {
                    launchAppActivity(context, data.packageName, data.activityClassName)
                }
            }
            OpenAppOrUrlData.TYPE_URL -> launchUrl(context, data.url)
            else -> {
                showToast(context.getString(R.string.launch_failed))
                false
            }
        }
    }

    fun launchUrl(context: Context, url: String): Boolean {
        return try {
            val normalizedUrl = normalizeOpenAppOrUrl(url) ?: run {
                showToast(context.getString(R.string.invalid_url))
                return false
            }
            if (normalizedUrl.startsWith("intent:")) {
                val intent = Intent.parseUri(normalizedUrl, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(context.packageManager) == null) {
                    showToast(context.getString(R.string.launch_failed))
                    return false
                }
                context.startActivity(intent)
                return true
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) == null) {
                showToast(context.getString(R.string.launch_failed))
                return false
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            showToast(context.getString(R.string.launch_failed))
            false
        }
    }

    fun normalizeOpenAppOrUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("intent:")) {
            return runCatching { Intent.parseUri(trimmed, Intent.URI_INTENT_SCHEME); trimmed }.getOrNull()
        }
        val hasExplicitScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(trimmed)
        val candidate = if (hasExplicitScheme || trimmed.contains("://")) trimmed else "https://$trimmed"
        val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
        return if (uri.scheme.isNullOrBlank()) null else candidate
    }

    fun launchAppInPopup(
        context: Context,
        packageName: String,
        className: String,
        horizontalBias: Float = DefaultMiniWindowHorizontalBias,
        verticalBias: Float = DefaultMiniWindowVerticalBias,
        verticalOffsetFraction: Float = DefaultMiniWindowVerticalOffsetFraction,
        widthFraction: Float = DefaultMiniWindowWidthFraction,
        heightFraction: Float = DefaultMiniWindowHeightFraction,
        overrideBounds: Boolean = false,
    ): Boolean {
        return MiniWindow.startActivity(
            context,
            ComponentName.createRelative(packageName, className),
            horizontalBias, verticalBias, verticalOffsetFraction,
            widthFraction, heightFraction,
            overrideBounds = overrideBounds,
        )
    }

    fun isMiniWindowSupported(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)
    }
}

private const val DefaultMiniWindowHorizontalBias = 0f
private const val DefaultMiniWindowVerticalBias = 0f
private const val DefaultMiniWindowVerticalOffsetFraction = 0f
private const val DefaultMiniWindowWidthFraction = 0.46f
private const val DefaultMiniWindowHeightFraction = 0.74f

private object MiniWindow {

    private fun getRealScreenSize(context: Context): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = wm.maximumWindowMetrics.bounds
        return Point(bounds.width(), bounds.height())
    }

    fun startActivity(
        context: Context,
        component: ComponentName,
        horizontalBias: Float,
        verticalBias: Float,
        verticalOffsetFraction: Float,
        widthFraction: Float,
        heightFraction: Float,
        overrideBounds: Boolean,
    ): Boolean {
        return try {
            val intent = Intent().apply {
                setComponent(component)
                setAction(Intent.ACTION_MAIN)
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val realSize = getRealScreenSize(context)
            val activityOptions = makeActivityOptions(
                horizontalBias, verticalBias, verticalOffsetFraction,
                widthFraction, heightFraction,
                realSize.x, realSize.y,
                overrideBounds = overrideBounds,
            )
            context.startActivity(intent, activityOptions.toBundle())
            true
        } catch (ignored: Exception) {
            showToast(context.getString(R.string.launch_mini_window_failed))
            false
        }
    }

    private fun makeActivityOptions(
        horizontalBias: Float,
        verticalBias: Float,
        verticalOffsetFraction: Float,
        widthFraction: Float,
        heightFraction: Float,
        realSw: Int,
        realSh: Int,
        overrideBounds: Boolean,
    ) = ActivityOptions.makeBasic().also { opts ->
        runCatching {
            opts.javaClass.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType).invoke(opts, 5)
        }
        val winW: Int
        val winH: Int
        val left: Int
        val top: Int
        if (overrideBounds) {
            winW = (realSw * widthFraction.coerceIn(0.2f, 1.5f)).roundToInt()
            winH = (realSh * heightFraction.coerceIn(0.2f, 1.5f)).roundToInt()
            left = ((realSw - winW) / 2f + realSw * horizontalBias.coerceIn(-1f, 1f)).roundToInt()
            top = ((realSh - winH) / 2f + realSh * verticalBias.coerceIn(-1f, 1f)).roundToInt()
        } else {
            val scaledW = (realSw * 0.7f).roundToInt()
            winW = realSw
            winH = (realSw / 0.625f).roundToInt()
            left = ((realSw - scaledW) / 2f).roundToInt()
            top = ((realSh - winH) / 2f).roundToInt()
        }
        opts.setLaunchBounds(Rect(left, top, left + winW, top + winH))
    }
}
