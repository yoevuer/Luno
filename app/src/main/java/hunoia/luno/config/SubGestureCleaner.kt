package hunoia.luno.config

import hunoia.luno.action.api.ActionFacade
import hunoia.luno.config.model.Action
import hunoia.luno.config.model.DirectionActions
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
                    slideHoldActions = button.slideHoldActions.copy(
                        actions = button.slideHoldActions.actions.mapValues { (_, actions) -> actions.filterNot(shouldRemove) }
                    ),
                    longSlideActions = button.longSlideActions.copy(
                        actions = button.longSlideActions.actions.mapValues { (_, actions) -> actions.filterNot(shouldRemove) }
                    ),
                    longSlideHoldActions = button.longSlideHoldActions.copy(
                        actions = button.longSlideHoldActions.actions.mapValues { (_, actions) -> actions.filterNot(shouldRemove) }
                    ),
                    tapActions = button.tapActions.filterNot(shouldRemove),
                    doubleTapActions = button.doubleTapActions.filterNot(shouldRemove),
                    longPressActions = button.longPressActions.filterNot(shouldRemove),
                )
            }
        }

        ConfigProvider.updateGestureButtons { cleanActions(it) }
        ConfigProvider.updateSubGestureSettings { settings ->
            val cleanedSubGestures = settings.subGestures.map { gesture ->
                gesture.copy(
                    slideActions = gesture.slideActions.clean(shouldRemove),
                    slideHoldActions = gesture.slideHoldActions.clean(shouldRemove),
                    longSlideActions = gesture.longSlideActions.clean(shouldRemove),
                    longSlideHoldActions = gesture.longSlideHoldActions.clean(shouldRemove),
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

private fun DirectionActions.clean(shouldRemove: (Action) -> Boolean): DirectionActions {
    return copy(actions = actions.mapValues { (_, actions) -> actions.filterNot(shouldRemove) })
}
