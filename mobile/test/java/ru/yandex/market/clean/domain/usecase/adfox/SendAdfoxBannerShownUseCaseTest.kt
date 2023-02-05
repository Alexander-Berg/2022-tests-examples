package ru.yandex.market.clean.domain.usecase.adfox

import com.annimon.stream.Optional
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.data.repository.AdfoxRepository
import ru.yandex.market.domain.auth.model.AuthToken
import ru.yandex.market.domain.auth.model.authTokenTestInstance
import ru.yandex.market.domain.auth.model.uuidTestInstance
import ru.yandex.market.domain.auth.repository.AuthRepository
import ru.yandex.market.domain.auth.repository.UuidRepository
import ru.yandex.market.domain.auth.usecase.GetAuthTokenUseCase
import ru.yandex.market.domain.auth.usecase.GetUuidUseCase
import ru.yandex.market.mockResult

class SendAdfoxBannerShownUseCaseTest {

    private companion object {

        const val URL_MOCK = "yandex-market.ru/banner/visible/some_id"

        val UUID_MOCK = uuidTestInstance()
        val AUTH_TOKEN_MOCK = authTokenTestInstance()
        val OPTIONAL_AUTH_TOKEN_MOCK: Optional<AuthToken> = Optional.of(AUTH_TOKEN_MOCK)
    }

    private val adfoxRepository = mock<AdfoxRepository>()

    private val uuidRepository = mock<UuidRepository>()
    private val getUuidUseCase = GetUuidUseCase(uuidRepository)

    private val authRepository = mock<AuthRepository>()
    private val getAuthTokenUseCase = GetAuthTokenUseCase(authRepository)

    private val useCase = SendAdfoxBannerShownUseCaseImpl(

        adfoxRepository = adfoxRepository,
        uuidUseCase = getUuidUseCase,
        getAuthTokenUseCase = getAuthTokenUseCase
    )

    @Before
    fun setUp() {

        adfoxRepository.reportAdfoxBannerShown(URL_MOCK, UUID_MOCK, AUTH_TOKEN_MOCK).mockResult(Completable.complete())
        uuidRepository.getUuid().mockResult(Single.just(UUID_MOCK))
        authRepository.getCurrentAccountAuthToken().mockResult(Single.just(OPTIONAL_AUTH_TOKEN_MOCK))
    }

    @Test
    fun `check sending adfox banner shown on complete`() {

        useCase.execute(URL_MOCK)
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(adfoxRepository).reportAdfoxBannerShown(URL_MOCK, UUID_MOCK, AUTH_TOKEN_MOCK)
        verify(uuidRepository).getUuid()
        verify(authRepository).getCurrentAccountAuthToken()
    }
}