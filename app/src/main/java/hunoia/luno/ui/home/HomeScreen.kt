package hunoia.luno.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import hunoia.luno.R
import hunoia.luno.config.model.GestureButton
import hunoia.luno.config.model.SubGesture
import hunoia.luno.bridge.intent.gotoAccessibilitySettings
import hunoia.luno.ui.home.UiEvent
import hunoia.luno.ui.theme.SectionPadding
import hunoia.luno.ui.theme.Spacing12
import hunoia.luno.ui.component.MyColumn
import hunoia.luno.ui.component.TopBar
import hunoia.luno.ui.component.color.ColorPickerBottomSheet
import hunoia.luno.ui.component.color.ColorSelection
import hunoia.luno.ui.theme.resolveColor
import hunoia.luno.config.model.ThemeColorKey
import kotlin.math.roundToInt



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavToGestureButtonSettings: (GestureButton) -> Unit,
    onNavToSubGestureEditor: (String) -> Unit,
    onNavToPointerSettings: () -> Unit = {},
    onNavToFrozenManage: () -> Unit = {},
    onNavToAppBlacklist: () -> Unit = {},
    onNavToActionLibrary: () -> Unit = {},
    onNavToActionSettings: () -> Unit = {},
    vm: HomeVM = viewModel()
) {
        val scrollState = rememberScrollState()
        var showResetConfirm by remember { mutableStateOf(false) }
        var colorPickerTarget by remember { mutableStateOf<Any?>(null) }
        var colorPickerColor by remember { mutableStateOf(Color.Transparent) }
        var myColumnWindowY by remember { mutableIntStateOf(0) }
        var cardAreaWindowY by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        onDispose {
            vm.collapseAll()
        }
    }
    val context = LocalContext.current
    UDFComponent(
        component = vm.udfComponent,
        onEvent = { event ->
            when (event) {
                is UiEvent.ScrollToBottom -> {
                    scrollState.animateScrollTo(
                        value = scrollState.maxValue,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                }
                is UiEvent.ScrollToEvent -> {
                    scrollState.animateScrollTo(
                        value = event.offsetY,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                }

            }
        }
    ) { uiState ->
        val createFileLauncher = rememberLauncherForActivityResult(
            contract = CreateDocument("*/*")
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            vm.backup(context, uri)
        }
        val getFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            vm.restore(context, uri)
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(key1 = lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.updatePermissionState()
            }
        }

            Box {
                if (colorPickerTarget != null) {
                    val scheme = MaterialTheme.colorScheme
                    val themeColorArgb = remember(scheme) {
                        ThemeColorKey.entries.associateWith { it.resolveColor(scheme).toArgb() }
                    }
                    ColorPickerBottomSheet(
                        onDismissRequest = { colorPickerTarget = null },
                        onColorSelected = { selection ->
                            when (selection) {
                                is ColorSelection.Custom -> {
                                    when (val target = colorPickerTarget) {
                                        is GestureButton -> vm.updateGestureButtonColor(target, selection.color.toArgb())
                                        is SubGesture -> vm.updateSubGestureColor(target, selection.color.toArgb())
                                    }
                                }
                                is ColorSelection.Theme -> {
                                    themeColorArgb[selection.key]?.let { resolvedArgb ->
                                        when (val target = colorPickerTarget) {
                                            is GestureButton -> vm.updateGestureButtonColor(target, resolvedArgb)
                                            is SubGesture -> vm.updateSubGestureColor(target, resolvedArgb)
                                        }
                                    }
                                }
                            }
                            colorPickerTarget = null
                        },
                        initialColor = colorPickerColor,
                    )
                }

                Scaffold(topBar = {
                    TopBar(
                        onBack = { },
                        title = stringResource(id = R.string.home_title),
                        showBackIcon = false,
                        actions = {}
                    )
                }) { padding ->
                    MyColumn(
                        modifier = Modifier
                            .padding(padding)
                            .onGloballyPositioned { coords ->
                                myColumnWindowY = coords.positionInWindow().y.roundToInt()
                            },
                        scrollState = scrollState,
                    ) {
                        HomeHeroCard(
                            uiState = uiState,
                            onClick = {
                                if (uiState.isGestureEnabled) {
                                    vm.onAppGestureEnabledChange(false)
                                } else if (!uiState.isAccessibilityEnabled) {
                                    context.gotoAccessibilitySettings()
                                } else {
                                    vm.onAppGestureEnabledChange(true)
                                }
                            },
                            onCheckedChange = {
                                if (it && !uiState.isAccessibilityEnabled) {
                                    context.gotoAccessibilitySettings()
                                } else {
                                    vm.onAppGestureEnabledChange(it)
                                }
                            }
                        )

                        Spacer(Modifier.height(SectionPadding))

                        HomeKeepAliveCard(
                            enabled = uiState.isKeepAliveEnabled,
                            onCheckedChange = { vm.onKeepAliveChange(it) },
                        )

                        Spacer(Modifier.height(Spacing12))

                        HomeFeatureGrid(
                            uiState = uiState,
                            onExcludeClick = onNavToAppBlacklist,
                            onActionSettingsClick = onNavToActionSettings,
                            onActionLibraryClick = onNavToActionLibrary,
                            onPointerClick = onNavToPointerSettings,
                            onFrozenClick = onNavToFrozenManage,
                            onFreezeClick = { vm.oneKeyFreeze() },
                            onUnfreezeClick = { vm.oneKeyUnfreeze() },
                            onBackupClick = {
                                val appName = context.getString(context.applicationInfo.labelRes)
                                val date = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                                createFileLauncher.launch("${appName}_$date.zip")
                            },
                            onRestoreClick = { getFileLauncher.launch("*/*") },
                            onResetToggle = { showResetConfirm = !showResetConfirm },
                            showResetConfirm = showResetConfirm,
                            onResetConfirm = {
                                vm.reset()
                                showResetConfirm = false
                            },
                            onResetDismiss = { showResetConfirm = false },
                            onCardAreaPosition = { cardAreaWindowY = it },
                        )

                        Spacer(Modifier.height(SectionPadding))

        LaunchedEffect(
                            uiState.isGestureButtonListExpanded,
                            uiState.isSubGestureListExpanded,
                        ) {
                            if (uiState.isGestureButtonListExpanded ||
                                uiState.isSubGestureListExpanded
                            ) {
                                kotlinx.coroutines.delay(120)
                                scrollState.animateScrollTo(
                                    scrollState.maxValue,
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        easing = FastOutSlowInEasing,
                                    ),
                                )
                            }
                        }

                        LaunchedEffect(showResetConfirm) {
                            if (showResetConfirm) {
                                kotlinx.coroutines.delay(120)
                                if (cardAreaWindowY > 0 && myColumnWindowY > 0) {
                                    val targetScroll = (cardAreaWindowY - myColumnWindowY - 120).coerceAtLeast(0)
                                    if (targetScroll > scrollState.value) {
                                        scrollState.animateScrollTo(
                                            targetScroll,
                                            animationSpec = tween(
                                                durationMillis = 400,
                                                easing = FastOutSlowInEasing,
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        HomeGestureSections(
                            uiState = uiState,
                            onGestureHeaderClick = {
                                if (uiState.isGestureButtonListExpanded) {
                                    vm.expandGestureButtonList(false)
                                } else {
                                    vm.expandSubGestureList(false)
                                    vm.expandGestureButtonList(true)
                                }
                            },
                            onSubHeaderClick = {
                                if (uiState.isSubGestureListExpanded) {
                                    vm.expandSubGestureList(false)
                                } else {
                                    vm.expandGestureButtonList(false)
                                    vm.expandSubGestureList(true)
                                }
                            },
                            onGestureButtonClick = onNavToGestureButtonSettings,
                            onSubGestureClick = onNavToSubGestureEditor,
                            onGestureCheckedChange = { button, enabled -> vm.onGestureButtonEnabledChange(button, enabled) },
                            onSubCheckedChange = { gesture, enabled -> vm.onSubGestureEnabledChange(gesture, enabled) },
                            onAddGesture = { vm.addGestureButton() },
                            onAddSub = {
                                val id = java.util.UUID.randomUUID().toString()
                                vm.addSubGesture(id)
                            },
                            onMarkColorClick = { target ->
                                colorPickerTarget = target
                                colorPickerColor = when (target) {
                                    is GestureButton -> Color(target.color)
                                    is SubGesture -> Color(target.color)
                                    else -> Color.Transparent
                                }
                            },
                            onGestureButtonRename = { button ->
                                vm.showRenameDialog(RenameTarget.GestureButton(button = button))
                            },
                            onSubGestureRename = { gesture ->
                                vm.showRenameDialog(RenameTarget.SubGesture(gesture = gesture))
                            },
                        )
                    }

                    RenameDialog(
                        target = uiState.renameDialogTarget,
                        onDismissRequest = { vm.hideRenameDialog() },
                        onConfirm = { target, name -> vm.doRename(target, name) },
                    )
                }

                GestureButtonOverlay(
                    visible = uiState.isGestureButtonListExpanded,
                    gestureButtons = uiState.gestureButtons,
                )
            }
    }
}
