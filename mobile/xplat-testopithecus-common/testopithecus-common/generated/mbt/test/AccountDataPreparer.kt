// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/test/account-data-preparer.ts >>>

package com.yandex.xplat.testopithecus.common

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*

public interface AccountDataPreparer {
    fun prepare(account: OAuthUserAccount): XPromise<Unit>
}

public abstract class AccountDataPreparerProvider<T: AccountDataPreparer> {
    abstract fun provide(lockedAccount: UserAccount, type: AccountType2): T
    abstract fun provideModelDownloader(fulfilledPreparers: YSArray<T>, accountsWithTokens: YSArray<OAuthUserAccount>): AppModelProvider
}

