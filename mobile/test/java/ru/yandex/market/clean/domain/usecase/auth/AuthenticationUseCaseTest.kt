package ru.yandex.market.clean.domain.usecase.auth

import com.annimon.stream.Exceptional
import com.annimon.stream.Optional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.AuthRepositoryImpl
import ru.yandex.market.clean.domain.usecase.likedislike.LikeDislikeClearCounterCase
import ru.yandex.market.domain.auth.model.AuthToken
import ru.yandex.market.domain.auth.model.TokenType
import ru.yandex.market.domain.auth.model.accountIdTestInstance
import ru.yandex.market.domain.auth.model.autoLoginResultTestInstance
import ru.yandex.market.domain.auth.model.loginParamsTestInstance
import ru.yandex.market.domain.auth.model.loginResultTestInstance
import ru.yandex.market.domain.auth.usecase.CheckIsLoggedInUseCase
import ru.yandex.market.domain.auth.usecase.CheckIsLoggedOutUseCase
import ru.yandex.market.domain.auth.usecase.GetAuthStatusStreamUseCase
import ru.yandex.market.domain.auth.usecase.LoginUseCase
import ru.yandex.market.utils.Observables
import ru.yandex.market.utils.asExceptional
import ru.yandex.market.utils.asOptional
import ru.yandex.market.utils.wrapIntoExceptional

@RunWith(MockitoJUnitRunner::class)
class AuthenticationUseCaseTest {

    private val authRepository = mock<AuthRepositoryImpl>()

    private val loginUseCase = LoginUseCase(
        authRepository
    )
    private val checkIsLoggedInUseCase = CheckIsLoggedInUseCase(
        authRepository
    )
    private val checkIsLoggedOutUseCase = CheckIsLoggedOutUseCase(
        authRepository
    )
    private val getAuthStatusStreamUseCase = GetAuthStatusStreamUseCase(
        authRepository
    )
    private val likeDislikeClearCounterCase = mock<LikeDislikeClearCounterCase>()
    private val useCase = AuthenticationUseCase(
        authRepository,
        getAuthStatusStreamUseCase,
        likeDislikeClearCounterCase
    )

    @Test
    fun `Logout by user intent request logout from repository`() {
        whenever(authRepository.logout()).thenReturn(Completable.complete())

        useCase.logout()
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(authRepository).logout()
    }

    @Test
    fun `Login by account id request login from repository`() {
        val loginResult = loginResultTestInstance()
        whenever(authRepository.loginWithParams(any())).thenReturn(Single.just(loginResult))

        val params = loginParamsTestInstance()
        loginUseCase.loginWithParams(params)
            .test()
            .assertNoErrors()
            .assertResult(loginResult)

        verify(authRepository).loginWithParams(params)
    }

    @Test
    fun `Try auto login request auto login from repository`() {
        val autoLoginResult = autoLoginResultTestInstance()
        whenever(authRepository.tryAutoLogin()).thenReturn(Single.just(autoLoginResult))

        loginUseCase.tryAutoLogin()
            .test()
            .assertResult(autoLoginResult)

        verify(authRepository).tryAutoLogin()
    }

    @Test
    fun `Logout request logout from repository`() {
        whenever(authRepository.logout()).thenReturn(Completable.complete())

        useCase.logout()
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(authRepository).logout()
    }

    @Test
    fun `Is logged in pass value from repository`() {
        val isLoggedIn = true
        whenever(authRepository.getIsLoggedInSingle()).thenReturn(Single.just(isLoggedIn))

        checkIsLoggedInUseCase.isLoggedIn()
            .test()
            .assertResult(isLoggedIn)
    }

    @Test
    fun `Is logged out pass reverted value from repository`() {
        val isLoggedIn = true
        whenever(authRepository.getIsLoggedInSingle()).thenReturn(Single.just(isLoggedIn))

        checkIsLoggedOutUseCase.isLoggedOut()
            .test()
            .assertResult(!isLoggedIn)
    }

    @Test
    fun `Current auth token pass first value from repository`() {
        val firstToken = AuthToken(TokenType.OAUTH, "first")
        whenever(authRepository.deprecatedAuthTokenStream).thenReturn(
            Observables.stream(
                firstToken,
                AuthToken(TokenType.OAUTH, "second")
            )
                .wrapIntoExceptional()
                .map { it as Exceptional<out AuthToken?> }
        )

        useCase.currentAuthToken
            .test()
            .assertResult(firstToken.asExceptional())
    }

    @Test
    fun `Authentication status stream pass stream from repository`() {
        val stream = Observable.never<Boolean>()
        whenever(authRepository.getAuthStatusStream()).thenReturn(stream)

        val resultStream = getAuthStatusStreamUseCase.getAuthStatusStream()

        assertThat(resultStream).isSameAs(stream)
    }

    @Test
    fun `Wait for authentication waits until logged in status is true`() {
        val stream = PublishSubject.create<Boolean>()
        whenever(authRepository.getAuthStatusStream()).thenReturn(stream)

        val testObserver = useCase.waitForAuthentication.test()
        stream.onNext(false)
        testObserver.assertNotComplete()

        stream.onNext(true)
        testObserver
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `Logout when removed account id equals to passed id`() {
        whenever(authRepository.getCurrentAccountIdStream())
            .thenReturn(Observables.stream(accountIdTestInstance().asOptional()))
        val logoutCallable = mock<() -> Unit>()
        whenever(authRepository.logout()).thenReturn(Completable.fromCallable(logoutCallable))

        useCase.handleRemoveAccount(accountIdTestInstance())
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(logoutCallable).invoke()
    }

    @Test
    fun `Don't logout when removed account id not equals to passed id`() {
        whenever(authRepository.getCurrentAccountIdStream())
            .thenReturn(Observables.stream(accountIdTestInstance(id = 1L).asOptional()))

        useCase.handleRemoveAccount(accountIdTestInstance(id = 2L))
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(authRepository, never()).logout()
    }

    @Test
    fun `Don't logout when current account is empty`() {
        whenever(authRepository.getCurrentAccountIdStream()).thenReturn(Observables.stream(Optional.empty()))

        useCase.handleRemoveAccount(accountIdTestInstance())
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(authRepository, never()).logout()
    }
}