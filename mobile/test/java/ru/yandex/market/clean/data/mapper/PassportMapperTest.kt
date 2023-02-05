package ru.yandex.market.clean.data.mapper

import com.yandex.passport.api.PassportUid
import org.junit.Test
import ru.yandex.market.passport.mapper.PassportEnvironmentMapper
import org.assertj.core.api.Assertions.assertThat
import ru.yandex.market.domain.auth.model.AccountId
import ru.yandex.market.domain.auth.model.Environment

class PassportMapperTest {

    private val environmentMapper = PassportEnvironmentMapper()
    private val mapper = PassportMapper(environmentMapper)

    @Test
    fun `Check if account id mapping from passport uid works properly`() {
        val accountSnapShot = AccountId(1L, Environment.TESTING)
        val passportUid = mapper.createPassportUid(accountSnapShot)

        val mapped = mapper.mapAccountId(passportUid)

        assertThat(mapped).extracting { it.id }.isEqualTo(accountSnapShot.id)
    }

    @Test
    fun `Check if create passport uid from account id works properly`() {
        val accountSnapShot = AccountId(1L, Environment.TESTING)
        val passportUid = PassportUid.Factory.from(
            environmentMapper.map(accountSnapShot.environment),
            accountSnapShot.id
        )

        val created = mapper.createPassportUid(accountSnapShot)
        assertThat(created).extracting { it.value }.isEqualTo(passportUid.value)
    }
}