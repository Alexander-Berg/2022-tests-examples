package com.yandex.mail.network.tasks

import com.yandex.mail.fakeserver.CustomResponseRule
import com.yandex.mail.network.MailApiException
import com.yandex.mail.network.json.response.StatusWrapper
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.tools.Accounts
import com.yandex.mail.util.AuthErrorException
import com.yandex.mail.util.BaseIntegrationTest
import com.yandex.mail.util.PermErrorException
import com.yandex.mail.util.TempErrorException
import io.reactivex.observers.TestObserver
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations

@RunWith(IntegrationTestRunner::class)
class PurgeTaskTest : BaseIntegrationTest() {

    @Before
    fun setUp() {
        init(Accounts.testLoginData)
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `purging of negative mids does not calling mail api`() {
        val testObs = TestObserver<RecordedRequest>()
        account.observeRecordedRequestsFor("purge_items").subscribe(testObs)
        val task = createPurgeTask(-1, -2, -3)
        val returnedStatus = task.performNetworkOperationRetrofit(app)
        val expectedStatus = StatusWrapper().apply { status = StatusWrapper.Status.OK }
        assertThat(returnedStatus).isEqualTo(expectedStatus)
        testObs
            .assertSubscribed()
            .assertNoValues()
    }

    @Test
    fun `purging of mixed mids filters positive mids to call api`() {
        val testObs = TestObserver<RecordedRequest>()
        account.observeRecordedRequestsFor("purge_items").subscribe(testObs)
        val task = createPurgeTask(-1, -2, -3, 1, 2, 3)
        val returnedStatus = task.performNetworkOperationRetrofit(app)
        val expectedStatus = StatusWrapper().apply { status = StatusWrapper.Status.OK }
        assertThat(returnedStatus).isEqualTo(expectedStatus)
        testObs
            .assertSubscribed()
            .assertValue { rec -> rec.requestUrl!!.queryParameter("mids").equals("1,2,3") }
    }

    @Test
    fun `on 500 throws temp error`() {
        testHelperNetworkPartThrowsError(500)
            .isInstanceOf(TempErrorException::class.java)
    }


    @Test
    fun `on 400 throws PermErr`() {
        testHelperNetworkPartThrowsError(400)
            .isInstanceOf(PermErrorException::class.java)
    }

    @Test
    fun `on 499 throws PermErr`() {
        testHelperNetworkPartThrowsError(499)
            .isInstanceOf(PermErrorException::class.java)
    }

    @Test
    fun `on 401 throws AuthErr`() {
        testHelperNetworkPartThrowsError(401)
            .isInstanceOf(AuthErrorException::class.java)
    }


    @Test
    fun `on 300 should throws MailApiException`() {
        testHelperNetworkPartThrowsError(300)
            .isInstanceOf(MailApiException::class.java)
    }

    fun createPurgeTask(vararg messageIds: Long): PurgeTask {
        val task = PurgeTask(
            app,
            messageIds.toList(),
            user.uid
        )
        return task
    }


    fun testHelperNetworkPartThrowsError(statusCode: Int): AbstractThrowableAssert<*, out Throwable> {
        account.addCustomResponseRule(apiV2GeneralRule(statusCode))
        val task = createPurgeTask(1, 2)
        return assertThatThrownBy { task.performNetworkOperationRetrofit(app) }
    }


    private fun apiV2GeneralRule(statusCode: Int): CustomResponseRule = object : CustomResponseRule {
        override fun match(request: RecordedRequest): Boolean {
            return true
        }

        override fun getResponse(request: RecordedRequest): MockResponse {
            return MockResponse().setResponseCode(statusCode)
        }
    }

}
