package com.yandex.mail.model

import com.yandex.mail.container.AccountInfoContainer
import com.yandex.mail.entity.AccountEntity
import dagger.Module
import dagger.Provides
import io.reactivex.Flowable
import javax.inject.Singleton

class TestCrossAccountModel(accountModel: AccountModel, generalSettingsModel: GeneralSettingsModel) :
    CrossAccountModel(accountModel, generalSettingsModel) {

    override fun observeUboxAccounts(): Flowable<List<AccountEntity>> {
        return Flowable.just(emptyList())
    }

    override fun observeUboxWithEmails(): Flowable<List<AccountInfoContainer>> {
        return Flowable.just(emptyList())
    }
}

@Module
class TestCrossAccountModule : CrossAccountModule() {

    @Provides
    @Singleton
    override fun provideCrossAccountModel(accountModel: AccountModel, generalSettingsModel: GeneralSettingsModel): CrossAccountModel {
        return TestCrossAccountModel(accountModel, generalSettingsModel)
    }
}
