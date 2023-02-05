package ru.yandex.market.test.runner

import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import io.qameta.allure.espresso.AllureAndroidListener
import ru.yandex.market.test.TestCase
import ru.yandex.market.test.TestMarketApplication
import ru.yandex.market.test.util.FailedTestsListener
import ru.yandex.market.test.util.debug
import ru.yandex.market.test.util.error

@Suppress("unused")
class MarketTestInstrumentationRunner : AndroidJUnitRunner() {

    private var arguments: Bundle? = null

    override fun onCreate(arguments: Bundle?) {
        this.arguments = arguments
        arguments?.putCharSequence("listener", arguments.getCharSequence("listener")
            ?.let {
                "$it,${AllureAndroidListener::class.java.name},${FailedTestsListener::class.java.name}"
            }
            ?: "${AllureAndroidListener::class.java.name},${FailedTestsListener::class.java.name}")
        logArguments(arguments)
        super.onCreate(arguments)
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return Instrumentation.newApplication(TestMarketApplication::class.java, context)
    }

    override fun callApplicationOnCreate(app: Application?) {
        app?.initializeInitialStates()
        super.callApplicationOnCreate(app)
    }

    private fun Application.initializeInitialStates() {
        if (this is TestMarketApplication) {
            try {
                val testCaseName = arguments?.getString(ARGS_KEY_CLASS_NAME)?.substringBefore(METHOD_DELIMITER)
                require(!testCaseName.isNullOrEmpty()) {
                    "Не удалось вытащить имя TestCase'а из аргументов"
                }
                val testCase = Class.forName(testCaseName).newInstance() as TestCase
                setInitialStates(testCase.states)
                debug("Вызвали setInitialStates со следующимими состояниями: " + testCase.states)
            } catch (e: Exception) {
                error("Не удалось создать инстанс TestCase'а", e)
            }
        } else {
            error("Класс $this не является наследником TestMarketApplication!")
        }
    }

    override fun onStart() {
        //Must prevent all fails because of modal dialogs
        targetContext.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

        super.onStart()
    }

    private fun logArguments(arguments: Bundle?) {
        debug("Received ${arguments?.size() ?: 0} arguments")

        arguments?.keySet()?.forEachIndexed { index, key ->
            debug("$index $key : ${arguments[key]}")
        }
    }

    companion object {
        private const val METHOD_DELIMITER = '#'
        private const val ARGS_KEY_CLASS_NAME = "class"
    }
}