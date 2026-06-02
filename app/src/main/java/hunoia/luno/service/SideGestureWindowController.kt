package hunoia.luno.service

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import hunoia.luno.config.model.GestureButton
import hunoia.luno.gesture.input.MotionEventDispatcher
import hunoia.luno.bridge.window.removeWindow
import hunoia.luno.bridge.window.removeWindows
import hunoia.luno.bridge.window.setBasic
import hunoia.luno.bridge.window.updateLayout
import hunoia.luno.bridge.window.updateMainView
import hunoia.luno.bridge.DensityProvider
import hunoia.luno.gesture.mirroredButton

internal data class GestureButtonWindowTarget(
    val sourceButton: GestureButton,
    val windowButton: GestureButton,
)

class SideGestureWindowController(private val host: SideGestureService) {
    var mainView: View? = null
        private set

    var buttonViews: List<View>? = null
        private set

    var subGestureOverlayView: View? = null
        private set

    var actionPanelOverlayView: View? = null
        private set

    fun attachActionPanelOverlay() {
        if (actionPanelOverlayView != null) return
        val wm = ContextCompat.getSystemService(host, WindowManager::class.java)!!
        val lp = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            @android.annotation.SuppressLint("RtlHardcoded")
            gravity = Gravity.LEFT or Gravity.TOP
            x = 0
            y = 0
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        val view = View(host).apply {
            setOnTouchListener { _, event ->
                MotionEventDispatcher.dispatch(event)
                true
            }
        }
        wm.addView(view, lp)
        actionPanelOverlayView = view
    }

    fun detachActionPanelOverlay() {
        val view = actionPanelOverlayView ?: return
        host.removeWindow(view)
        actionPanelOverlayView = null
    }

    fun detachTransientOverlays() {
        detachActionPanelOverlay()
        detachSubGestureOverlay()
    }

    fun replaceMainOverlay(content: @Composable () -> Unit) {
        mainView?.let { host.removeWindow(it) }
        mainView = attachComposeOverlay(content)
    }

    fun replaceGestureButtons(buttons: Collection<GestureButton>) {
        buttonViews?.let { host.removeWindows(it) }
        buttonViews = buttons.flatMap { button ->
            buildList {
                add(attachGestureButton(GestureButtonWindowTarget(button, button)))
                button.mirroredButton()?.let { mirroredButton ->
                    if (mirroredButton.bounds != button.bounds) {
                        add(attachGestureButton(GestureButtonWindowTarget(button, mirroredButton)))
                    }
                }
            }
        }
    }

    fun updateMainLayout() {
        val view = mainView ?: return
        val lp = (view.layoutParams as WindowManager.LayoutParams).apply { updateMainView() }
        updateWindowLayout(view, lp)
    }

    fun attachSubGestureOverlay(center: Offset, radiusPx: Int) {
        val lp = createSubGestureLayoutParams(center, radiusPx)
        subGestureOverlayView?.let { view ->
            updateWindowLayout(view, lp)
            return
        }
        val wm = ContextCompat.getSystemService(host, WindowManager::class.java)!!
        val view = View(host).apply {
            setOnTouchListener { _, event ->
                MotionEventDispatcher.dispatch(event)
                true
            }
        }
        wm.addView(view, lp)
        subGestureOverlayView = view
    }

    private fun createSubGestureLayoutParams(center: Offset, radiusPx: Int): WindowManager.LayoutParams {
        val screenWidth = DensityProvider.screenWidthPx
        val screenHeight = DensityProvider.screenHeightPx
        val radius = radiusPx.coerceAtLeast(DensityProvider.dp2px(48f))
        val size = (radius * 2).coerceAtMost(maxOf(screenWidth, screenHeight))
        val validCenter = if (center.x.isFinite() && center.y.isFinite()) center else Offset(screenWidth / 2f, screenHeight / 2f)
        return WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.RGBA_8888
            width = size.coerceAtMost(screenWidth)
            height = size.coerceAtMost(screenHeight)
            x = (validCenter.x - width / 2f).roundToInt().coerceIn(0, (screenWidth - width).coerceAtLeast(0))
            y = (validCenter.y - height / 2f).roundToInt().coerceIn(0, (screenHeight - height).coerceAtLeast(0))
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            @android.annotation.SuppressLint("RtlHardcoded")
            gravity = Gravity.LEFT or Gravity.TOP
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    fun detachSubGestureOverlay() {
        val view = subGestureOverlayView ?: return
        host.removeWindow(view)
        subGestureOverlayView = null
    }

    fun updateWindowLayout(view: View, lp: WindowManager.LayoutParams) {
        host.updateLayout(view, lp)
    }

    private fun attachComposeOverlay(content: @Composable () -> Unit): ComposeView {
        val wm = ContextCompat.getSystemService(host, WindowManager::class.java)!!
        val lp = WindowManager.LayoutParams().apply {
            setBasic(false)
            updateMainView()
        }
        val composeView = ComposeView(host).apply {
            setViewTreeLifecycleOwner(host)
            setViewTreeViewModelStoreOwner(host)
            setViewTreeSavedStateRegistryOwner(host)
            setContent { content() }
        }
        wm.addView(composeView, lp)
        return composeView
    }

    private fun attachGestureButton(target: GestureButtonWindowTarget): View {
        val wm = ContextCompat.getSystemService(host, WindowManager::class.java)!!
        val lp = WindowManager.LayoutParams().apply {
            setBasic(target.sourceButton.enabled)
            updateGestureButton(target.windowButton)
        }
        val view = View(host).apply {
            tag = target
            setOnTouchListener { v, event ->
                MotionEventDispatcher.dispatch(event)
                if (event.action == MotionEvent.ACTION_UP) v.performClick()
                true
            }
        }
        wm.addView(view, lp)
        return view
    }
}

internal fun WindowManager.LayoutParams.updateGestureButton(button: GestureButton) {
    updateGestureButton(
        button = button,
        rootWidth = DensityProvider.screenWidthPx,
        rootHeight = DensityProvider.screenHeightPx
    )
}

internal fun WindowManager.LayoutParams.updateGestureButton(
    button: GestureButton,
    rootWidth: Int,
    rootHeight: Int,
) {
    val left = (rootWidth * button.bounds.x).roundToInt().coerceIn(0, rootWidth)
    val top = (rootHeight * button.bounds.y).roundToInt().coerceIn(0, rootHeight)
    val right = (rootWidth * (button.bounds.x + button.bounds.width)).roundToInt().coerceIn(left, rootWidth)
    val bottom = (rootHeight * (button.bounds.y + button.bounds.height)).roundToInt().coerceIn(top, rootHeight)
    width = (right - left).coerceAtLeast(1)
    height = (bottom - top).coerceAtLeast(1)
    x = left
    y = top
    @android.annotation.SuppressLint("RtlHardcoded")
    gravity = android.view.Gravity.LEFT or android.view.Gravity.TOP
}
