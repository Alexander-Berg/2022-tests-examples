// Copyright (c) 2019 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.account

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.domain.account.management.SharedAccount
import ru.yandex.direct.repository.account.SharedAccountLocalRepository
import ru.yandex.direct.repository.account.SharedAccountQuery
import ru.yandex.direct.repository.account.SharedAccountRemoteRepository
import ru.yandex.direct.web.AuthorizationDataProvider

class SharedAccountInteractorTest {

    private val accountName = "accountName"

    private val apiAccount = SharedAccount().apply { accountID = 1 }

    private val localAccount = SharedAccount().apply { accountID = 0 }

    private lateinit var scheduler: TestScheduler

    private lateinit var observer: TestObserver<SharedAccount>

    private lateinit var interactor: SharedAccountInteractor

    private lateinit var localRepo: SharedAccountLocalRepository

    private lateinit var remoteRepo: SharedAccountRemoteRepository

    private lateinit var authDataProvider: AuthorizationDataProvider

    @Before
    fun runBeforeAnyTest() {
        scheduler = TestScheduler()
        observer = TestObserver()
        localRepo = mock { on { select(any()) } doReturn listOf(apiAccount) }
        remoteRepo = mock { on { fetch(any()) } doReturn listOf(apiAccount) }
        authDataProvider = mock { on { accountName } doReturn accountName }
        interactor = SharedAccountInteractor(localRepo, remoteRepo, scheduler, scheduler, authDataProvider)
    }

    @Test
    fun updateDailyBudget_shouldCallRepoToMakeUpdate() {
        interactor.updateDailyBudget(localAccount).subscribe(observer)
        scheduler.triggerActions()
        verify(remoteRepo).updateDailyBudget(localAccount)
    }

    @Test
    fun updateDailyBudget_shouldFetchSharedAccountFromApi() {
        interactor.updateDailyBudget(localAccount).subscribe(observer)
        scheduler.triggerActions()
        verify(remoteRepo).fetch(SharedAccountQuery.forClient(accountName))
    }

    @Test
    fun updateDailyBudget_shouldSaveSharedAccountToDb() {
        interactor.updateDailyBudget(localAccount).subscribe(observer)
        scheduler.triggerActions()
        verify(localRepo).update(SharedAccountQuery.forClient(accountName), listOf(apiAccount))
    }

    @Test
    fun updateDailyBudget_shouldReturnSharedAccountFromApi() {
        interactor.updateDailyBudget(localAccount).subscribe(observer)
        scheduler.triggerActions()
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue(apiAccount)
    }
}