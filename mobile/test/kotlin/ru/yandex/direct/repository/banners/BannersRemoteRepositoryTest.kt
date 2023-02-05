// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.repository.banners

import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import retrofit2.Call
import ru.yandex.direct.utils.MockedDirectApi5
import ru.yandex.direct.web.ApiInstanceHolder
import ru.yandex.direct.web.api5.IDirectApi5
import ru.yandex.direct.web.api5.request.BaseAction
import ru.yandex.direct.web.api5.result.ActionResult
import ru.yandex.direct.web.api5.result.BaseArrayResult
import ru.yandex.direct.web.api5.result.BaseResult
import ru.yandex.direct.web.exception.ActionException
import ru.yandex.direct.web.exception.ApiException

class BannersRemoteRepositoryTest {
    val bannerId = 0L

    val warning = ActionResult.Warning(500, "message", "description")

    val warningResult = ActionResult.warning(bannerId, warning)

    val errorResult = ActionResult.error(bannerId, warning)

    val successResult = ActionResult.success(bannerId)

    @Test
    fun makeAction_doesNotThrowActionException_onApiWarning() {
        val api = ApiInstanceHolder.just(returning(warningResult))
        val repo = BannersRemoteRepository(api, mock())
        assertThatCode { repo.resume(listOf(0)) }.doesNotThrowAnyException()
        assertThatCode { repo.suspend(listOf(0)) }.doesNotThrowAnyException()
    }

    @Test
    fun makeAction_throwsActionException_onApiError() {
        val api = ApiInstanceHolder.just(returning(errorResult))
        val repo = BannersRemoteRepository(api, mock())
        assertThatThrownBy { repo.resume(listOf(0)) }
                .isExactlyInstanceOf(ActionException::class.java)
                .hasMessage(warning.message)
        assertThatThrownBy { repo.suspend(listOf(0)) }
                .isExactlyInstanceOf(ActionException::class.java)
                .hasMessage(warning.message)
    }

    @Test
    fun makeAction_returnsTrue_onSuccess() {
        val api = ApiInstanceHolder.just(returning(successResult))
        val repo = BannersRemoteRepository(api, mock())
        assertThat(repo.resume(listOf(0))).isTrue()
        assertThat(repo.suspend(listOf(0))).isTrue()
    }

    @Test
    fun makeAction_throwsApiException_onFailure() {
        val api = ApiInstanceHolder.just(returningApiError())
        val repo = BannersRemoteRepository(api, mock())
        assertThatThrownBy { repo.resume(listOf(0)) }
                .isExactlyInstanceOf(ApiException::class.java)
        assertThatThrownBy { repo.suspend(listOf(0)) }
                .isExactlyInstanceOf(ApiException::class.java)
    }

    private fun returning(actionResult: ActionResult): IDirectApi5 = object : MockedDirectApi5() {
        override fun action(endpoint: String, body: BaseAction): Call<BaseArrayResult<ActionResult>> {
            return delegate
                    .returningResponse(BaseArrayResult(actionResult))
                    .action(endpoint, body)
        }
    }

    private fun returningApiError(): IDirectApi5 = object : MockedDirectApi5() {
        override fun action(endpoint: String, body: BaseAction): Call<BaseArrayResult<ActionResult>> {
            return delegate
                    .returningResponse(BaseResult.error(500))
                    .action(endpoint, body)
        }
    }
}