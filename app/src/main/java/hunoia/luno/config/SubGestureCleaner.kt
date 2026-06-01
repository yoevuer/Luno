package hunoia.luno.config

import hunoia.luno.action.api.ActionFacade
import hunoia.luno.config.model.Action
import hunoia.luno.config.model.GestureButton
import kotlinx.serialization.json.Json

object SubGestureCleaner {

    suspend fun cleanSubGestureReferences(
        deletedId: String,
        shouldRemove: (Action) -> Boolean,
    ) {
        fun cleanActions(buttons: List<GestureButton>): List<GestureButton> {
            return buttons.map { button ->
                button.copy(
                    slideActions = button.slideActions.copy(
                        actions = button.slideActions.actions.mapValues { (_, actions) -> actions.filterNot(shouldRemove) }
                    ),
                    longSlideActions = button.longSlideActions.copy(
                        actions = button.longSlideActions.actions.mapValues { (_, actions) -> actions.filterNot(shouldRemove) }
                    ),
                    tapActions = button.tapActions.filterNot(shouldRemove),
                    longPressActions = button.longPressActions.filterNot(shouldRemove),
                )
            }
        }

        ConfigProvider.updateGestureButtons { cleanActions(it) }
        ConfigProvider.updateSubGestureSettings { settings ->
            fun clean(action: Action?) = action?.takeUnless(shouldRemove)
            val cleanedSubGestures = settings.subGestures.map { gesture ->
                gesture.copy(
                    upAction = clean(gesture.upAction),
                    downAction = clean(gesture.downAction),
                    leftAction = clean(gesture.leftAction),
                    rightAction = clean(gesture.rightAction),
                    upRightAction = clean(gesture.upRightAction),
                    downRightAction = clean(gesture.downRightAction),
                    downLeftAction = clean(gesture.downLeftAction),
                    upLeftAction = clean(gesture.upLeftAction),
                )
            }
            settings.copy(subGestures = cleanedSubGestures)
        }
    }

    fun isSubGestureAction(action: Action): Boolean =
        action.value == ActionFacade.SUB_GESTURE

    fun matchesDeletedSubGesture(action: Action, deletedId: String): Boolean {
        if (!isSubGestureAction(action)) return false
        return try {
            val data = Json.decodeFromString<hunoia.luno.action.payload.SubGestureActionData>(action.data)
            data.id == deletedId
        } catch (_: Exception) {
            false
        }
    }
}
