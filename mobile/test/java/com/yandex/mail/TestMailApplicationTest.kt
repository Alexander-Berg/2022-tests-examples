package com.yandex.mail

import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.util.ignore
import io.reactivex.Completable
import io.reactivex.exceptions.OnErrorNotImplementedException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(IntegrationTestRunner::class)
class TestMailApplicationTest {

    @Test
    fun `unhandled rx exception must be rethrown`() {
        assertThatThrownBy {
            Completable.fromAction { throw TestRuntimeException() }.subscribe()
        }
            .isInstanceOf(InternalError::class.java)
            .hasCauseExactlyInstanceOf(OnErrorNotImplementedException::class.java)
    }

    @Test
    fun `handled rx exception should not be rethrown`() {
        Completable.fromAction { throw TestRuntimeException() }.subscribe(
            {
                // success
            },
            { error ->
                assertThat(error)
                    .isInstanceOf(TestRuntimeException::class.java)
                    .hasMessage("Synthetic exception for test purpose")
                    .hasNoCause()
            }
        ).ignore()
    }
}
