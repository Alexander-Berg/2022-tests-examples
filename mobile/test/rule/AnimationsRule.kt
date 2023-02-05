package ru.yandex.market.test.rule

import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class AnimationsRule(
    private val uiDevice: UiDevice,
    private val enableAnimations: Boolean
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return if (enableAnimations) {
            EnableStatement(base)
        } else {
            DisableStatement(base)
        }
    }

    private fun enableAnimations() {
        with(uiDevice) {
            executeShellCommand(ENABLE_TRANSITION_SHELL_COMMAND)
            executeShellCommand(ENABLE_WINDOW_SCALE_SHELL_COMMAND)
            executeShellCommand(SET_ANIMATOR_DURATION_TO_1_SHELL_COMMAND)
        }
    }

    private fun disableAnimations() {
        with(uiDevice) {
            executeShellCommand(DISABLE_TRANSITION_SHELL_COMMAND)
            executeShellCommand(DISABLE_WINDOW_SCALE_SHELL_COMMAND)
            executeShellCommand(SET_ANIMATOR_DURATION_TO_0_SHELL_COMMAND)
        }
    }


    private inner class EnableStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            try {
                enableAnimations()
                base.evaluate()
            } finally {
                disableAnimations()
            }
        }
    }

    private inner class DisableStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            try {
                disableAnimations()
                base.evaluate()
            } finally {
                enableAnimations()
            }
        }
    }

    private companion object {
        const val ENABLE_TRANSITION_SHELL_COMMAND = "settings put global transition_animation_scale 1"
        const val ENABLE_WINDOW_SCALE_SHELL_COMMAND = "settings put global window_animation_scale 1"
        const val SET_ANIMATOR_DURATION_TO_1_SHELL_COMMAND = "settings put global animator_duration_scale 1"

        const val DISABLE_TRANSITION_SHELL_COMMAND = "settings put global transition_animation_scale 0"
        const val DISABLE_WINDOW_SCALE_SHELL_COMMAND = "settings put global window_animation_scale 0"
        const val SET_ANIMATOR_DURATION_TO_0_SHELL_COMMAND = "settings put global animator_duration_scale 0"
    }
}