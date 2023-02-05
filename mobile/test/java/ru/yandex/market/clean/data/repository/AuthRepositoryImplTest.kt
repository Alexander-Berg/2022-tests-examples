package ru.yandex.market.clean.data.repository

import com.annimon.stream.Exceptional
import com.annimon.stream.Optional
import com.yandex.passport.api.PassportUid
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.PassportTestData
import ru.yandex.market.clean.data.health.facades.AuthHealthFacade
import ru.yandex.market.clean.data.mapper.PassportMapper
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.domain.auth.model.AuthToken
import ru.yandex.market.domain.auth.model.TokenType
import ru.yandex.market.domain.auth.model.accountIdTestInstance
import ru.yandex.market.domain.auth.model.authTokenTestInstance
import ru.yandex.market.domain.auth.model.autoLoginResultTestInstance
import ru.yandex.market.domain.auth.model.loginParamsTestInstance
import ru.yandex.market.domain.auth.model.userAccountTestInstance
import ru.yandex.market.internal.PreferencesDataStore
import ru.yandex.market.manager.AuthManager
import ru.yandex.market.passport.model.AuthAccount
import ru.yandex.market.test.extensions.asObservable
import ru.yandex.market.utils.Observables
import ru.yandex.market.utils.asExceptional
import ru.yandex.market.utils.asOptional

class AuthRepositoryImplTest {
    private val authManager = mock<AuthManager>()
    private val workerScheduler = WorkerScheduler(Schedulers.trampoline())
    private val passportMapper = mock<PassportMapper>()
    private val preferencesDataStore = mock<PreferencesDataStore>()
    private val authHealthFacade = mock<AuthHealthFacade>()
    private val repository = AuthRepositoryImpl(
        authManager,
        workerScheduler,
        passportMapper,
        preferencesDataStore,
        authHealthFacade
    )

    @Test
    fun `Return authorized when current account is present`() {
        whenever(authManager.currentAccountStream)
            .thenReturn(Observable.just(AuthAccount.testInstance().asOptional()))
        whenever(passportMapper.mapAccount(any())).thenReturn(userAccountTestInstance())

        repository.getAuthStatusStream()
            .test()
            .assertValue(true)
    }

    @Test
    fun `Return unauthorized when current account is present`() {
        whenever(authManager.currentAccountStream)
            .thenReturn(Optional.empty<AuthAccount>().asObservable())

        repository.getAuthStatusStream()
            .test()
            .assertValue(false)
    }

    @Test
    fun `Distinct authorization status until changed`() {
        whenever(authManager.currentAccountStream)
            .thenReturn(
                Observable.just(
                    AuthAccount.testInstance().asOptional(),
                    AuthAccount.testInstance().asOptional(),
                    Optional.empty()
                )
            )
        whenever(passportMapper.mapAccount(any()))
            .thenReturn(userAccountTestInstance())
            .thenReturn(userAccountTestInstance())

        repository.getAuthStatusStream()
            .test()
            .assertValues(true, false)
    }

    @Test
    fun `Requests auth token from authentication manager`() {
        val authToken = AuthToken(TokenType.OAUTH, "token")
        whenever(authManager.getAuthToken(any())).thenReturn(authToken)
        whenever(passportMapper.createPassportUid(any())).thenReturn(PassportTestData.getPassportUid())

        repository.getAuthToken(accountIdTestInstance())
            .test()
            .assertResult(authToken)
    }

    @Test
    fun `getAuthTokenStream distinct accounts until changed`() {
        val firstUid = PassportTestData.getPassportUid(value = 1L)
        val secondUid = PassportTestData.getPassportUid(value = 2L)
        whenever(authManager.currentPassportUidStream)
            .thenReturn(Observables.stream(firstUid, secondUid).map { it.asOptional() })
        whenever(authManager.getAuthToken(any())).thenReturn(AuthToken(TokenType.OAUTH, "token"))
        whenever(passportMapper.mapAccount(any())).thenReturn(userAccountTestInstance())

        repository.deprecatedAuthTokenStream
            .test()
            .assertValueCount(1)
    }

    @Test
    fun `getAuthTokenStream maps account stream into token stream`() {
        val firstUid = PassportTestData.getPassportUid(value = 1L)
        val secondUid = PassportTestData.getPassportUid(value = 2L)
        whenever(authManager.currentPassportUidStream).thenReturn(
            Observables.stream(firstUid, secondUid).map { it.asOptional() }
        )
        val firstToken = AuthToken(TokenType.OAUTH, "firstToken")
        val secondToken = AuthToken(TokenType.OAUTH, "secondToken")
        whenever(authManager.getAuthToken(firstUid)).thenReturn(firstToken)
        whenever(authManager.getAuthToken(secondUid)).thenReturn(secondToken)

        repository.deprecatedAuthTokenStream
            .test()
            .assertValues(firstToken.asExceptional(), secondToken.asExceptional())
    }

    @Test
    fun `getAuthTokenStream returns exceptional containing null when account is not present`() {
        whenever(authManager.currentPassportUidStream).thenReturn(Observables.stream(Optional.empty()))

        repository.deprecatedAuthTokenStream
            .test()
            .assertValue(Exceptional.of<AuthToken> { null })
    }

    @Test
    fun `getAuthTokenStream returns error exceptional in case of errors during obtain token`() {
        whenever(authManager.currentPassportUidStream)
            .thenReturn(Observables.stream<Optional<PassportUid>>(PassportTestData.getPassportUid().asOptional()))
        whenever(authManager.getAuthToken(any())).thenThrow(RuntimeException())

        repository.deprecatedAuthTokenStream
            .test()
            .assertValue { !it.isPresent && it.exception is RuntimeException }
    }

    @Test
    fun `Logout by user intent request logout from authentication manager`() {
        whenever(authManager.logout()).thenReturn(Completable.complete())

        repository.logout()
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(authManager).logout()
    }

    @Test
    fun `Login by account id request login from authentication manager`() {
        whenever(authManager.getAuthToken(any())).thenReturn(authTokenTestInstance())
        whenever(authManager.login(any())).thenReturn(Single.just(AuthAccount.testInstance()))
        whenever(passportMapper.mapAccount(any())).thenReturn(userAccountTestInstance())

        val params = loginParamsTestInstance()
        repository.loginWithParams(params)
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(authManager).login(params)
    }

    @Test
    fun `Try auto login request auto login from authentication manager`() {
        val passportAutoLoginResult = PassportTestData.getPassportAutoLoginResult()
        whenever(authManager.tryAutoLogin()).thenReturn(Single.just(passportAutoLoginResult))
        val authToken = AuthToken(TokenType.OAUTH, "authToken")
        whenever(authManager.getAuthToken(any())).thenReturn(authToken)
        val autoLoginResult = autoLoginResultTestInstance()
        whenever(passportMapper.mapAutoLoginResult(passportAutoLoginResult, authToken)).thenReturn(autoLoginResult)

        repository.tryAutoLogin()
            .test()
            .assertResult(autoLoginResult)

        verify(authManager).tryAutoLogin()
    }

    @Test
    fun `Logout request logout from authentication manager`() {
        whenever(authManager.logout()).thenReturn(Completable.complete())

        repository.logout()
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(authManager).logout()
    }

    @Test
    fun `Current account id stream return account id from authentication manager`() {
        whenever(authManager.currentPassportUidStream)
            .thenReturn(Observables.stream(PassportTestData.getPassportUid().asOptional()))
        val accountId = accountIdTestInstance()
        whenever(passportMapper.mapAccountId(any())).thenReturn(accountId)

        repository.getCurrentAccountIdStream()
            .test()
            .assertNoErrors()
            .assertValue(accountId.asOptional())
            .assertNotComplete()
    }

    @Test
    fun `Current account id stream return empty optional when there is no account`() {
        whenever(authManager.currentPassportUidStream).thenReturn(Observables.stream(Optional.empty()))

        repository.getCurrentAccountIdStream()
            .test()
            .assertNoErrors()
            .assertValue(Optional.empty())
            .assertNotComplete()
    }

    @Test
    fun `Current account id stream distinct values until changed`() {
        whenever(authManager.currentPassportUidStream)
            .thenReturn(
                Observables.stream(
                    PassportTestData.getPassportUid(),
                    PassportTestData.getPassportUid()
                ).map { it.asOptional() })
        val accountId = accountIdTestInstance()
        whenever(passportMapper.mapAccountId(any())).thenReturn(accountId)

        repository.getCurrentAccountIdStream()
            .test()
            .assertNoErrors()
            .assertValue(accountId.asOptional())
            .assertNotComplete()
    }
}