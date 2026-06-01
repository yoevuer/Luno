package hunoia.luno.service

import android.app.WallpaperManager
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import com.aaron.composeaccessibility.ComponentAccessibilityService
import hunoia.luno.config.model.ActionSettings
import hunoia.luno.config.model.AdvancedSettings
import hunoia.luno.config.model.GestureSettings
import hunoia.luno.config.model.InitialSettings
import hunoia.luno.core.AppContext
import hunoia.luno.core.Events
import hunoia.luno.bridge.WallpaperChangedEvent
import hunoia.luno.quicklaunch.QuickLaunchFacade
import hunoia.luno.pointer.PointerRuntime
import hunoia.luno.pointer.PointerFacade
import hunoia.luno.service.runtime.VolumeScrubRuntime
import hunoia.luno.service.runtime.GestureButtonHideRuntime
import java.lang.ref.WeakReference
import hunoia.luno.config.ConfigProvider
import hunoia.luno.service.QuickAppLauncherOverlay
import hunoia.luno.service.QuickAppLauncherOverlayHost
import hunoia.luno.service.RuntimePanelOverlay
import hunoia.luno.service.RuntimePanelOverlayHost
import hunoia.luno.pointer.PointerOverlayHost
import hunoia.luno.freeze.FrozenPackageEnabler
import hunoia.luno.config.model.GestureButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SideGestureService : ComponentAccessibilityService(), SideGestureRuntime, QuickAppLauncherOverlayHost, RuntimePanelOverlayHost, PointerOverlayHost {

    companion object {
        private var currentRef: WeakReference<SideGestureService>? = null
        var current: SideGestureService?
            get() = currentRef?.get()
            private set(value) { currentRef = if (value != null) WeakReference(value) else null }
    }

    private val proxy = SideGestureServiceProxy(this)
    override val context: android.content.Context
        get() = this

    val quickAppLauncherOverlay by lazy {
        QuickAppLauncherOverlay(this).apply {
            onAppLaunchRequested = { app ->
                coroutineScope.launch(Dispatchers.IO) {
                    recordQuickAppLaunchIfSuccess(true, "${app.packageName}/${app.className}") {
                        ConfigProvider.recordQuickAppLaunch(it)
                    }
                }
            }
            QuickLaunchFacade.showOverlay = { show() }
        }
    }
    val runtimePanelOverlay by lazy {
        RuntimePanelOverlay(this)
    }
    override val coroutineScope = MainScope()
    private val windowController = SideGestureWindowController(this)
    private val buttonRefreshCoordinator = SideGestureButtonRefreshCoordinator(
        host = this,
        scopeProvider = { coroutineScope },
        initialSettingsProvider = { ConfigProvider.getInitialSettings() },
        advancedSettingsProvider = { advancedSettings },
        buttonViewsProvider = { windowController.buttonViews },
        runtimeStateProvider = {
            SideGestureRuntimeState(
                currentPackageName = getCurrentPackageName(),
                isNowInLockScreenPage = isNowInLockScreenPage,
                isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE,
                isInLauncher = nowInLauncher(),
                isKeyboardInputActive = isKeyboardInputActive,
                hiddenGestureButtons = hideRuntime.getSnapshot(),
                isMouseMode = pointerRuntime.isActive,
                nowMs = SystemClock.uptimeMillis(),
            )
        },
    )
    private val settingsObserver = SideGestureSettingsObserver(
        scope = coroutineScope,
        onInitialSettings = { initialSettings = it },
        onAdvancedSettings = { advancedSettings = it },
        onGestureSettings = { gestureSettings = it },
        onActionSettings = { actionSettings = it },
        onGestureButtons = { buttons ->
            windowController.replaceGestureButtons(buttons)
            updateGestureButtons()
        },
        onRefreshGestureButtons = { updateGestureButtons() }
    )
    private val frozenPackageEnabler = FrozenPackageEnabler(
        context = this,
        scopeProvider = { coroutineScope },
        log = { message -> android.util.Log.d("LunoLauncher", message) }
    )
    private val systemBroadcastObserver = SystemBroadcastObserver(
        context = this,
        onScreenOff = {
            isNowInLockScreenPage = true
            quickAppLauncherOverlay.closeImmediately()
            runtimePanelOverlay.closeImmediately()
            updateGestureButtons()
        },
        onUserPresent = {
            isNowInLockScreenPage = false
            updateGestureButtons()
        }
    )
    private var wallpaperColorsListener: WallpaperManager.OnColorsChangedListener? = null
    private val pointerRuntime = PointerRuntime(
        host = this,
        scope = coroutineScope,
        gestureSettingsProvider = { gestureSettings },
        onStateChanged = { updateGestureButtons(delayMs = 0L) },
    ).also { PointerFacade.runtimeProvider = { it } }
    private val volumeScrubRuntime = VolumeScrubRuntime(
        context = this,
        actionSettingsProvider = { actionSettings },
        onStateChanged = { updateGestureButtons() },
    )
    private val hideRuntime = GestureButtonHideRuntime(
        scope = coroutineScope,
        onStateChanged = { updateGestureButtons() },
    )

    private var orientation = if (AppContext.get().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
    private var isNowInLockScreenPage = false
    private var isKeyboardInputActive = false
    private var refreshJob: Job? = null

    var initialSettings: InitialSettings? = null
        private set
    override var advancedSettings: AdvancedSettings? = null
        private set
    var gestureSettings: GestureSettings? = null
        private set
    var actionSettings: ActionSettings? = null
        private set

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (orientation != newConfig.orientation) {
            orientation = newConfig.orientation
            updateMainLayout()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        proxy.onAccessibilityEvent(event)
        event?.let { updateKeyboardInputState(it) }
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            updateGestureButtons()
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        if (current === this) currentRef = null
        quickAppLauncherOverlay.closeImmediately()
        runtimePanelOverlay.close()
        frozenPackageEnabler.release()
        coroutineScope.cancel()
        pointerRuntime.onDestroy()
        volumeScrubRuntime.onDestroy()
        proxy.onRelease()
        systemBroadcastObserver.unregister()
        wallpaperColorsListener?.let { listener ->
            WallpaperManager.getInstance(this).removeOnColorsChangedListener(listener)
        }
    }

    override fun onSetOverlay() {
        current = this
        registerRuntimeObservers()
        windowController.replaceMainOverlay { renderMainOverlay() }
        settingsObserver.start()
        coroutineScope.launch(Dispatchers.IO) {
            QuickLaunchFacade.queryApps(this@SideGestureService)
        }
    }

    private fun registerRuntimeObservers() {
        systemBroadcastObserver.register()
        val listener = WallpaperManager.OnColorsChangedListener { _, _ ->
            Events.post(WallpaperChangedEvent())
        }
        wallpaperColorsListener = listener
        WallpaperManager.getInstance(this).addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
    }

    @Composable
    private fun renderMainOverlay() {
        val screenshotService = this
        GestureOverlayView(
            screenshotService = screenshotService,
            onSubGestureModeChanged = { inSubGesture ->
                if (inSubGesture) windowController.attachSubGestureOverlay()
                else windowController.detachSubGestureOverlay()
            },
            onAction = { action, sourceButton, sourceOverride ->
                proxy.onAction(action, sourceButton, sourceOverride)
            },
            onPointerStart = { settings -> pointerRuntime.beginBridge(settings) },
            onPointerEnd = { pointerRuntime.end() },
            onPointerActionAtPosition = { x, y, keepActive ->
                pointerRuntime.performBridgeActionAt(x, y, keepActive)
            },
            windowController = windowController,
        )
    }

    private fun updateMainLayout() {
        windowController.updateMainLayout()
        updateGestureButtons()
    }

    internal fun updateWindowLayout(view: View, lp: WindowManager.LayoutParams) {
        windowController.updateWindowLayout(view, lp)
    }

    private fun updateGestureButtons(delayMs: Long = 100L) {
        refreshJob?.cancel()
        refreshJob = coroutineScope.launch {
            if (delayMs > 0L) delay(delayMs)
            buttonRefreshCoordinator.refresh()
        }
    }

    private fun updateKeyboardInputState(event: AccessibilityEvent) {
        val active = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> isEditableInput(event.source) || hasEditableInputFocus()
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> hasEditableInputFocus()
            else -> return
        }
        setKeyboardInputActive(active)
    }

    private fun setKeyboardInputActive(active: Boolean) {
        if (isKeyboardInputActive == active) return
        isKeyboardInputActive = active
        updateGestureButtons()
    }

    private fun hasEditableInputFocus(): Boolean {
        return isEditableInput(rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT))
    }

    private fun isEditableInput(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val className = node.className?.toString().orEmpty()
        return node.isEditable ||
            className.endsWith("EditText") ||
            node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT.id }
    }

    fun getCurrentPackageName(): String {
        return rootInActiveWindow?.packageName?.toString() ?: ""
    }

    override fun nowInLauncher(): Boolean {
        return QuickLaunchFacade.isLauncherPackage(this, getCurrentPackageName())
    }

    override fun requestEnableFrozenPackage(packageName: String, onResult: (Boolean) -> Unit) {
        frozenPackageEnabler.request(packageName, onResult)
    }

    fun showPointerOverlay(continuousModeOverride: Boolean? = null): Boolean =
        pointerRuntime.show(continuousModeOverride)

    fun beginPointerMode(): Boolean = pointerRuntime.show()  // dummy: no-op, handled by runtime

    fun endPointerMode() = pointerRuntime.end()

    fun showVolumeScrubOverlay(): Boolean = volumeScrubRuntime.show()

    fun beginVolumeScrubMode(): Boolean = volumeScrubRuntime.show()  // dummy: no-op

    fun endVolumeScrubMode() = volumeScrubRuntime.end()

    fun hideGestureButtonTemporarily(button: GestureButton, delayMs: Long) {
        hideRuntime.hideTemporarily(button, delayMs)
    }

}

internal suspend fun recordQuickAppLaunchIfSuccess(
    success: Boolean,
    appKey: String,
    record: suspend (String) -> Unit,
) {
    if (success) {
        record(appKey)
    }
}
