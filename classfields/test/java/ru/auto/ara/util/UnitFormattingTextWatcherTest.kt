package ru.auto.ara.util

import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.widget.EditText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.core_ui.util.addTextWatcher
import ru.auto.data.util.NBSP
import ru.auto.data.util.RUB_UNICODE
import ru.auto.test.activity.TestEmptyActivity
import ru.auto.test.runner.AllureRobolectricRunner
import kotlin.test.assertEquals


@RunWith(AllureRobolectricRunner::class)
class UnitFormattingTextWatcherTest {

    @JvmField
    @Rule
    val rule = ActivityScenarioRule(TestEmptyActivity::class.java)

    @Test
    fun shouldDeleteCharacter() {
        setupEditTextWithWatcherAndText("10$NBSP$RUB_UNICODE") { et ->

            et.setSelection(2)
            et.simulateKeyCodeInput(KeyEvent.KEYCODE_DEL)

            assertEquals("1$NBSP$RUB_UNICODE", et.text.toString())
        }
    }

    @Test
    fun shouldDeleteDigitCharacterWhenDeleteNBSP() {
        setupEditTextWithWatcherAndText("10$NBSP$RUB_UNICODE") { et ->

            et.setSelection(3)
            et.simulateKeyCodeInput(KeyEvent.KEYCODE_DEL)

            assertEquals("1$NBSP$RUB_UNICODE", et.text.toString())
        }
    }

    @Test
    fun shouldDeleteDigitCharacterWhenDeleteRUB() {
        setupEditTextWithWatcherAndText("10$NBSP$RUB_UNICODE") { et ->

            et.setSelection(4)
            et.simulateKeyCodeInput(KeyEvent.KEYCODE_DEL)

            assertEquals("1$NBSP$RUB_UNICODE", et.text.toString())
        }
    }

    @Test
    fun shouldClearWhenDeleteRUBWithLastDigit() {
        setupEditTextWithWatcherAndText("1$NBSP$RUB_UNICODE") { et ->

            et.setSelection(3)
            et.simulateKeyCodeInput(KeyEvent.KEYCODE_DEL)

            assertEquals("", et.text.toString())
        }
    }

    @Test
    fun shouldClearWhenDeleteNBSPWithLastDigit() {
        setupEditTextWithWatcherAndText("1$NBSP$RUB_UNICODE") { et ->

            et.setSelection(2)
            et.simulateKeyCodeInput(KeyEvent.KEYCODE_DEL)

            assertEquals("", et.text.toString())
        }
    }

    @Test
    fun shouldClearWhenDeleteLastDigit() {
        setupEditTextWithWatcherAndText("1$NBSP$RUB_UNICODE") { et ->

            et.setSelection(2)
            et.simulateKeyCodeInput(KeyEvent.KEYCODE_DEL)

            assertEquals("", et.text.toString())
        }
    }

    @Test
    fun shouldStayStillWhenDeleteOnEmptyString() {
        setupEditTextWithWatcherAndText("") { et ->

            et.setSelection(0)
            et.simulateKeyCodeInput(KeyEvent.KEYCODE_DEL)

            assertEquals("", et.text.toString())
        }
    }

    @Test
    fun shouldFormatInputOne() {
        setupEditTextWithWatcherAndText("") { et ->

            et.setSelection(0)
            et.simulateMultiInput("1")

            assertEquals("1$NBSP$RUB_UNICODE", et.text.toString())
        }
    }

    @Test
    fun shouldFormatInputThousands() {
        setupEditTextWithWatcherAndText("") { et ->

            et.setSelection(0)
            et.simulateMultiInput("1000")

            assertEquals("1 000$NBSP$RUB_UNICODE", et.text.toString())
        }
    }

    private fun setupEditTextWithWatcherAndText(text: String, block: (EditText) -> Unit) = rule.scenario.onActivity { activity ->
        val et = EditText(activity)
        et.setText(text)
        val tw = UnitFormattingTextWatcher(et, RUB_UNICODE, {})
        et.addTextWatcher(tw)
        block(et)
    }

    private fun EditText.simulateMultiInput(text: String) {
        val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(text.toCharArray())
        for (event in events) {
            dispatchKeyEvent(event)
        }
    }

    private fun EditText.simulateKeyCodeInput(keyCode: Int) {
        dispatchKeyEvent(
            KeyEvent(
                0, 0, KeyEvent.ACTION_DOWN,
                keyCode, 0
            )
        )
        dispatchKeyEvent(
            KeyEvent(
                0, 0, KeyEvent.ACTION_UP,
                keyCode, 0
            )
        )
    }
}
