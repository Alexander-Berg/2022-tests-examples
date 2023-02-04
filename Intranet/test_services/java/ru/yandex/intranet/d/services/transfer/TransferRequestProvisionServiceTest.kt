package ru.yandex.intranet.d.services.transfer

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest

/**
 * TransferRequestProvisionServiceTest.
 *
 * @author Vladimir Zaytsev <vzay></vzay>@yandex-team.ru>
 * @since 13-12-2021
 */
@IntegrationTest
internal class TransferRequestProvisionServiceTest {
    @Autowired
    private lateinit var transferRequestProvisionService: TransferRequestProvisionService

    @Test
    fun test1() {
        assertNotNull(transferRequestProvisionService)
    }
}
