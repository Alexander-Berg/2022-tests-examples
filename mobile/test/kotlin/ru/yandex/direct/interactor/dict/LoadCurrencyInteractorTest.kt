// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.dict

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.repository.dicts.CurrencyLocalRepository
import ru.yandex.direct.repository.dicts.CurrencyQuery
import ru.yandex.direct.repository.dicts.CurrencyRemoteRepository

class LoadCurrencyInteractorTest {
    @Test
    fun loadCurrencyOnStartup_shouldForceLoadFromRemoteRepo() {
        val localRepo = mock<CurrencyLocalRepository> {
            on { select(CurrencyQuery.ofAllCurrencies()) } doReturn ApiSampleData.currency
        }

        val remoteRepo = mock<CurrencyRemoteRepository> {
            on { fetch(CurrencyQuery.ofAllCurrencies()) } doReturn ApiSampleData.currency
        }

        val scheduler = TestScheduler()

        val interactor = LoadCurrencyInteractor(localRepo, remoteRepo, scheduler, scheduler)

        interactor.loadCurrenciesOnStartup().subscribe()
        scheduler.triggerActions()

        verify(remoteRepo).fetch(CurrencyQuery.ofAllCurrencies())
        verify(localRepo).update(CurrencyQuery.ofAllCurrencies(), ApiSampleData.currency)
        verify(localRepo).select(CurrencyQuery.ofAllCurrencies())
    }
}