// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/test/regular-yandex-test-base.ts >>>

package com.yandex.xplat.testopithecus.common

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*

public abstract class RegularYandexTestBase<T: AccountDataPreparer> protected constructor(description: String, suite: YSArray<TestSuite> = mutableListOf(TestSuite.Fixed)): RegularTestBase<T>(description, AccountType2.Yandex, suite) {
}

