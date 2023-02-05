package ru.yandex.market.test.extensions

import com.yandex.passport.api.PassportAutoLoginMode
import com.yandex.passport.api.PassportAutoLoginProperties
import com.yandex.passport.api.PassportFilter
import com.yandex.passport.api.PassportTheme

class TestAutoLoginProperties : PassportAutoLoginProperties {

    override val filter: PassportFilter = TestPassportFilter()

    override val message: String? = null

    override val mode: PassportAutoLoginMode = PassportAutoLoginMode.EXACTLY_ONE_ACCOUNT

    override val theme: PassportTheme = PassportTheme.LIGHT
}
