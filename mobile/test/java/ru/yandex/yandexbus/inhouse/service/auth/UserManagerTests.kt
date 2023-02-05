package ru.yandex.yandexbus.inhouse.service.auth

import com.yandex.passport.api.PassportAccount
import com.yandex.passport.api.exception.PassportAccountNotAuthorizedException
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class UserManagerTests : BaseTest() {

    @Mock
    private lateinit var passportFacade: PassportFacade

    private lateinit var userManager: UserManager

    @Before
    override fun setUp() {
        super.setUp()
        userManager = UserManager(context, passportFacade, workerScheduler = Schedulers.immediate())
    }

    @Test
    fun userChanged() {
        val (account, user) = stubUserData()

        mockPassportFacadeAccountRetrieval(account, user.token)

        userManager.changeUser(user.id)
            .test()
            .assertValue(user)

        userManager.users()
            .test()
            .assertValue(user)
    }

    @Test
    fun userLoggedOut() {
        userManager.logout()
            .test()
            .assertCompleted()

        userManager.users()
            .test()
            .assertValue(User.Unauthorized)
    }

    @Test
    fun userMigrated() {
        val (account, user) = stubUserData()

        whenever(passportFacade.currentAccount()).thenReturn(account)

        mockPassportFacadeAccountRetrieval(account, user.token)

        userManager.users()
            .test()
            .awaitValueCount(1, 1, TimeUnit.SECONDS)
            .assertValue(user)
    }

    @Test
    fun userPersisted() {
        val (account, user) = stubUserData()

        mockPassportFacadeAccountRetrieval(account, user.token)

        userManager.changeUser(user.id)
            .test()
            .assertValue(user)

        val newUserManager =
            UserManager(context, passportFacade, workerScheduler = Schedulers.immediate())

        newUserManager.users()
            .test()
            .assertValue(user)
    }

    private fun mockPassportFacadeAccountRetrieval(
        account: PassportAccount,
        token: Token
    ) {
        val uid = account.uid.toUid()

        whenever(passportFacade.loadAccount(uid))
            .thenReturn(Single.just(account))

        whenever(passportFacade.receiveAuthToken(uid))
            .thenReturn(
                if (token is Token.Valid) {
                    Single.just(token.value)
                } else {
                    Single.error(PassportAccountNotAuthorizedException())
                }
            )
    }
}
