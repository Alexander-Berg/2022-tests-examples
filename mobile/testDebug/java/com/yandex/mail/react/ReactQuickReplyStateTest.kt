package com.yandex.mail.react

import com.yandex.mail.TestUtils
import com.yandex.mail.entity.MessageSmartReply
import com.yandex.mail.runners.IntegrationTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(IntegrationTestRunner::class)
class ReactQuickReplyStateTest {
    @Test
    fun `should serialize and deserialize object`() {
        val state = ReactQuickReplyState(
            "placeholder",
            "dismiss",
            mapOf(1L to false, 2L to true),
            mapOf(
                1L to listOf(
                    MessageSmartReply(1L, 1, "1: first"),
                    MessageSmartReply(1L, 2, "1: second")
                ),
                2L to listOf(
                    MessageSmartReply(2L, 1, "2: first"),
                    MessageSmartReply(2L, 2, "2: second")
                )
            ),
            mutableMapOf(
                2L to mutableSetOf(
                    MessageSmartReply(2L, 2, "second")
                )
            ),
            mutableMapOf(2L to "2: input")
        )

        val recreatedState = TestUtils.serializeAndDeserialize(state)

        assertThat(recreatedState.quickReplyPlaceholder).isEqualTo("placeholder")
        assertThat(recreatedState.smartReplyDismissTitle).isEqualTo("dismiss")
        assertThat(recreatedState.quickRepliesEnabledByMid).isEqualTo(mapOf(1L to false, 2L to true))
        assertThat(recreatedState.smartRepliesByMid).isEqualTo(
            mapOf(
                1L to listOf(
                    MessageSmartReply(1L, 1, "1: first"),
                    MessageSmartReply(1L, 2, "1: second")
                ),
                2L to listOf(
                    MessageSmartReply(2L, 1, "2: first"),
                    MessageSmartReply(2L, 2, "2: second")
                )
            )
        )
        assertThat(recreatedState.dismissedSmartRepliesByMid).isEqualTo(
            mutableMapOf(2L to mutableSetOf(MessageSmartReply(2L, 2, "second")))
        )
        assertThat(recreatedState.realInputByMid).isEqualTo(mutableMapOf(2L to "2: input"))
    }
}
