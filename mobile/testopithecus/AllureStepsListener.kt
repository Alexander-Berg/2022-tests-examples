package com.yandex.mail.testopithecus

import android.util.Log
import io.qameta.allure.kotlin.listener.StepLifecycleListener
import io.qameta.allure.kotlin.model.StepResult

public class AllureStepsListener : StepLifecycleListener {
    var stepLevel: Int = 1
    var tag = AllureStepsListener::class.java.simpleName

    public override fun beforeStepStart(result: StepResult) {
        Log.i(tag, "${"--".repeat(stepLevel)}> ${result.name} started")
        stepLevel += 1
        super.beforeStepStart(result)
    }

    public override fun afterStepStop(result: StepResult) {
        stepLevel -= 1
        Log.i(tag, "${"--".repeat(stepLevel)}> ${result.name} finished")
        super.afterStepStop(result)
    }
}
