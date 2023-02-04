package ru.auto.testextension

import com.google.gson.GsonBuilder
import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.util.ObjectUtils
import io.qameta.allure.kotlin.util.ResultsUtils

@Suppress("StringLiteralDuplication")
fun <S : Any> S.perform(description: String, block: Allure.StepContext.(S) -> Unit): S = step("Perform $description") {
    parameter("system under test", this@perform)
    block(this@perform)
    this@perform
}


@Suppress("StringLiteralDuplication")
fun <T> perform(description: String, block: Allure.StepContext.() -> T) = step("Perform $description", block)

@Suppress("StringLiteralDuplication")
fun <S> S.checkResult(description: String, block: Allure.StepContext.(S) -> Unit) = step("Check $description") {
    parameter("result", this@checkResult)
    block(this@checkResult)
    this@checkResult
}

@Suppress("StringLiteralDuplication")
fun <T> checkResult(description: String, block: Allure.StepContext.() -> T) = step("Check $description", block)

fun <T : Any> prepareParameter(name: String, parameter: T): T = step("Prepare $name") { parameterJson(name, parameter) }

fun <T : Any> Allure.StepContext.parameterJson(name: String, parameter: T): T {
    val gson = GsonBuilder().setPrettyPrinting().create()
    Allure.lifecycle.updateStep {
        val description = when {
            parameter.javaClass.isArray ||
                parameter.javaClass.isEnum ||
                parameter.javaClass.isPrimitive -> ObjectUtils.toString(parameter)
            else -> "${parameter.javaClass.simpleName}: ${gson.toJson(parameter)}"
        }
        val param = ResultsUtils.createParameter(name, description)
        it.parameters.add(param)
    }
    return parameter
}
