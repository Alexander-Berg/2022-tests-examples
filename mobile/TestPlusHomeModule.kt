package ru.yandex.market.di.module

import com.yandex.passport.api.PassportApi
import com.yandex.plus.home.api.AppExecutors
import com.yandex.plus.home.api.PurchaseController
import com.yandex.plus.home.api.settings.LocalSettingCallback
import com.yandex.plus.pay.PlusPay
import ru.yandex.market.clean.presentation.feature.plushome.sdk.PlusSdkComponentFacadeFactory
import ru.yandex.market.di.module.feature.PlusHomeModule
import ru.yandex.market.mocks.StateFacade
import ru.yandex.market.mocks.plushome.MockPlusSdkComponentFacadeFactory
import ru.yandex.market.util.AmConfigDataStore
import javax.inject.Inject

class TestPlusHomeModule @Inject constructor(private val stateFacade: StateFacade) : PlusHomeModule() {

    override fun providePlusSdkComponentFacadeFactory(
        purchaseController: PurchaseController,
        plusSdkAppExecutors: AppExecutors,
        plusSdkLocalSettings: LocalSettingCallback,
        amConfigDataStore: AmConfigDataStore,
        plusPay: PlusPay,
        passportApi: PassportApi
    ): PlusSdkComponentFacadeFactory {
        return MockPlusSdkComponentFacadeFactory(
            stateFacade
        )
    }
}
