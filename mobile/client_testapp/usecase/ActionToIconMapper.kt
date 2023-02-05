package ru.yandex.yandexnavi.annotations.client_testapp.usecase

import androidx.annotation.DrawableRes
import ru.yandex.yandexnavi.annotations.Action
import ru.yandex.yandexnavi.annotations.client_testapp.R

class ActionToIconMapper {
    @DrawableRes
    fun execute(action: Action): Int {
        return when (action) {
            Action.UNKNOWN -> R.drawable.notification_exit
            Action.STRAIGHT -> R.drawable.notification_straight
            Action.SLIGHT_LEFT -> R.drawable.notification_slight_left
            Action.SLIGHT_RIGHT -> R.drawable.notification_slight_right
            Action.LEFT -> R.drawable.notification_left
            Action.RIGHT -> R.drawable.notification_right
            Action.HARD_LEFT -> R.drawable.notification_hard_left
            Action.HARD_RIGHT -> R.drawable.notification_hard_right
            Action.FORK_LEFT -> R.drawable.notification_fork_left
            Action.FORK_RIGHT -> R.drawable.notification_fork_right
            Action.UTURN_LEFT -> R.drawable.notification_uturn_left
            Action.UTURN_RIGHT -> R.drawable.notification_uturn_right
            Action.ENTER_ROUNDABOUT -> R.drawable.notification_enter_roundabout
            Action.LEAVE_ROUNDABOUT -> R.drawable.notification_leave_roundabout
            Action.BOARD_FERRY -> R.drawable.notification_board_ferry
            Action.LEAVE_FERRY -> R.drawable.notification_leave_ferry
            Action.EXIT_LEFT -> R.drawable.notification_exit_left
            Action.EXIT_RIGHT -> R.drawable.notification_exit_right
            Action.FINISH -> R.drawable.notification_finish
            Action.WAYPOINT -> R.drawable.notification_finish
        }
    }
}
