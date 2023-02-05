package ru.yandex.market.test.extensions

import com.yandex.passport.api.Passport
import com.yandex.passport.api.PassportEnvironment
import com.yandex.passport.api.PassportFilter

class TestPassportFilter : PassportFilter {

    override val excludeLite: Boolean = false

    override val excludeSocial: Boolean = false

    override val includeMailish: Boolean = false

    override val includeMusicPhonish: Boolean = false

    override val includePhonish: Boolean = false

    override val onlyPdd: Boolean = false

    override val onlyPhonish: Boolean = false

    override val primaryEnvironment: PassportEnvironment = Passport.PASSPORT_ENVIRONMENT_PRODUCTION

    override val secondaryTeamEnvironment: PassportEnvironment? = null
}
