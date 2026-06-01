package hunoia.luno.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import hunoia.luno.R
import hunoia.luno.core.AppContext
import hunoia.luno.config.ConfigProvider
import hunoia.luno.config.model.AdvancedSettings
import hunoia.luno.config.model.FrozenAppSettings
import hunoia.luno.config.model.GestureSettings
import hunoia.luno.config.model.InitialSettings
import hunoia.luno.freeze.FreezeUseCase
import hunoia.luno.bridge.isAccessibilitySettingsOn
import hunoia.luno.bridge.feedback.showToast
import hunoia.luno.bridge.hasWriteSecureSettingsPermission
import hunoia.luno.service.DaemonService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class HomeVMBase : BaseComposeVM<UiState, UiEvent>() {

    fun onPointerChange(value: GestureSettings.Pointer) {
        updateUiState { it.copy(pointer = value) }
    }

    fun savePointerSettings() {
        viewModelScope.launch {
            ConfigProvider.updateGestureSettings { it.copy(pointer = uiState.pointer) }
        }
    }

    fun onPointerContinuousModeChange(value: Boolean) {
        onPointerChange(uiState.pointer.copy(continuousMode = value))
        savePointerSettings()
    }

    fun onPointerContinuousModeTimeoutChange(value: Long) {
        onPointerChange(uiState.pointer.copy(continuousModeTimeoutMs = value))
    }

    fun oneKeyFreeze() {
        viewModelScope.launch(
            CoroutineExceptionHandler { _, _ ->
                toast(R.string.bulk_freeze_failed)
            }
        ) {
            val result = FreezeUseCase.oneKeyFreeze()
            updateUiState { it.copy(frozenAppCount = result.totalAfter) }
            showToast(AppContext.get().getString(R.string.bulk_frozen_count, result.changed))
        }
    }

    fun oneKeyUnfreeze() {
        viewModelScope.launch(
            CoroutineExceptionHandler { _, _ ->
                toast(R.string.bulk_unfreeze_failed)
            }
        ) {
            val result = FreezeUseCase.oneKeyUnfreeze()
            updateUiState { it.copy(frozenAppCount = result.totalAfter) }
            showToast(AppContext.get().getString(R.string.bulk_unfrozen_count, result.changed))
        }
    }

    fun onKeepAliveChange(enabled: Boolean) {
        if (enabled) {
            val context = AppContext.get()
            if (!context.hasWriteSecureSettingsPermission()) {
                showToast(R.string.keep_alive_need_permission)
                return
            }
        }
        updateUiState { it.copy(isKeepAliveEnabled = enabled) }
        viewModelScope.launch {
            ConfigProvider.updateAdvancedSettings { it.copy(keepAliveEnabled = enabled) }
            withContext(Dispatchers.IO) {
                AppContext.get().getSharedPreferences("daemon", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("keep_alive", enabled)
                    .apply()
            }
            if (enabled) {
                val intent = Intent(AppContext.get(), DaemonService::class.java)
                AppContext.get().startForegroundService(intent)
            } else {
                val intent = Intent(AppContext.get(), DaemonService::class.java)
                AppContext.get().stopService(intent)
            }
        }
    }

    fun updatePermissionState() {
        viewModelScope.launch {
            val app = AppContext.get()
            val isGestureEnabled = ConfigProvider.getInitialSettings().gestureEnabled
            val clazz = Class.forName("hunoia.luno.service.SideGestureService")
            val isAccessibilityEnabled = app.isAccessibilitySettingsOn(clazz)
            val hasWriteSecureSettings = app.hasWriteSecureSettingsPermission()
            val keepAliveSettings = ConfigProvider.getAdvancedSettings().keepAliveEnabled
            updateUiState {
                it.copy(
                    isGestureEnabled = isAccessibilityEnabled && isGestureEnabled,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isKeepAliveEnabled = keepAliveSettings && hasWriteSecureSettings,
                )
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            ConfigProvider.resetAll()
        }
    }

    protected fun loadFrozenCount() {
        viewModelScope.launch {
            val count = FreezeUseCase.queryFrozenCount()
            updateUiState { it.copy(frozenAppCount = count) }
        }
    }

    protected fun saveSettings() {
        viewModelScope.launch {
            launch {
                ConfigProvider.updateInitialSettings {
                    it.copy(gestureEnabled = uiState.isGestureEnabled)
                }
            }
            launch {
                ConfigProvider.updateGestureButtons { uiState.gestureButtons }
            }
            launch {
                ConfigProvider.updateSubGestureSettings {
                    hunoia.luno.config.model.SubGestureSettings(subGestures = uiState.subGestures)
                }
            }
        }
    }

    protected fun loadData() {
        viewModelScope.launch {
            combine(
                ConfigProvider.initialSettings,
                ConfigProvider.gestureButtons,
                ConfigProvider.subGestureSettings,
                ConfigProvider.gestureSettings,
                ConfigProvider.advancedSettings,
                ConfigProvider.frozenAppSettings,
            ) { values ->
                val initial = values[0] as InitialSettings
                @Suppress("UNCHECKED_CAST")
                val buttons = values[1] as List<hunoia.luno.config.model.GestureButton>
                val subGestureSettings = values[2] as hunoia.luno.config.model.SubGestureSettings
                val gestureSettings = values[3] as GestureSettings
                val advancedSettings = values[4] as AdvancedSettings
                val frozenAppSettings = values[5] as FrozenAppSettings
                uiState.copy(
                    isGestureEnabled = initial.gestureEnabled,
                    gestureButtons = buttons.sortedBy { it.id },
                    subGestures = subGestureSettings.subGestures,
                    pointer = gestureSettings.pointer,
                    excludedAppCount = advancedSettings.excludeApps.size,
                    selectedFrozenAppCount = frozenAppSettings.oneKeyPackageNames.size,
                    isKeepAliveEnabled = advancedSettings.keepAliveEnabled,
                )
            }.collectLatest { state ->
                updateUiState { state }
            }
        }
    }
}
