package ru.auto.ara.test.log.adjust

import androidx.test.espresso.matcher.ViewMatchers.assertThat
import org.hamcrest.CoreMatchers.equalTo
import ru.auto.ara.core.actions.step
import ru.auto.ara.core.waiter.retry
import ru.auto.ara.util.statistics.MockedAdjustAnalyst

fun MockedAdjustAnalyst.waitLastEvent(token: String) = step("wait last adjust event is \"$token\"") {
    retry { assertThat("Adjust last token not equal to expected, got history: $history", history.lastOrNull(), equalTo(token)) }
}

fun MockedAdjustAnalyst.waitLastEventAndHistorySize(token: String, size: Int) =
    step("wait last adjust event is \"$token\" and size") {
        retry {
            assertThat("Adjust history size not equal to expected, got history: $history", history.size, equalTo(size))
            assertThat("Adjust last token not equal to expected, got history: $history", history.lastOrNull(), equalTo(token))
        }
    }

fun MockedAdjustAnalyst.waitEventsExactlyTheSame(tokens: List<String>) =
    step("assert adjust events exactly the same as \"$tokens\"") {
        retry { assertThat("Adjust tokens history not equal to expected, got history: $history", history, equalTo(tokens)) }
    }
