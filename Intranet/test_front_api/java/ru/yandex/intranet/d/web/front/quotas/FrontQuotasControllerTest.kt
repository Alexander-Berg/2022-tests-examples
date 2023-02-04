package ru.yandex.intranet.d.web.front.quotas

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.*
import ru.yandex.intranet.d.util.FrontStringUtil
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.AmountDto
import ru.yandex.intranet.d.web.model.FrontProvisionsDto
import java.math.BigDecimal

/**
 * FrontQuotasController test.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 09-06-2021
 */
@IntegrationTest
class FrontQuotasControllerTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    /**
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController.getAllBalances
     */
    @Test
    internal fun getAllBalancesTest() {
        val returnResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/quotas/_getAllBalances?folderId=" + TestFolders.TEST_FOLDER_1_ID + "&accountId=" + TestAccounts.TEST_ACCOUNT_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontProvisionsDto::class.java)
            .returnResult()

        val provisionsByResourceId = returnResult.responseBody!!.provisionsByResourceId
        assertEquals(3, provisionsByResourceId.size)
        assertFalse(provisionsByResourceId.containsKey(TestResources.YP_HDD_MYT))

        val provisions = provisionsByResourceId[TestResources.YP_SSD_MAN]!!
        assertEquals(valueOf(0), provisions.balance)
        assertEquals(valueOf(2), provisions.providedAbsolute)
        assertEquals(valueOf(1), provisions.providedRatio)
        assertEquals(valueOf(0), provisions.allocated)
        assertEquals(valueOf(0), provisions.allocatedRatio)
        assertEquals(UnitIds.TERABYTES, provisions.forEditUnitId)
        assertEquals(valueOf(2), provisions.providedDelta)
        assertEquals(valueOf(2000), provisions.providedAbsoluteInMinAllowedUnit)
        assertEquals(UnitIds.GIGABYTES, provisions.minAllowedUnitId)
        assertEquals(AmountDto("0", "GB",
            "0", "GB",
            "0", UnitIds.TERABYTES,
            "0", UnitIds.GIGABYTES), provisions.balanceAmount)
        assertEquals(AmountDto("2", "TB",
            "2000000000000", "B",
            "2", UnitIds.TERABYTES,
            "2000", UnitIds.GIGABYTES), provisions.deltaAmount)
        assertEquals(AmountDto("2", "TB",
            "2000000000000", "B",
            "2", UnitIds.TERABYTES,
            "2000", UnitIds.GIGABYTES), provisions.providedAmount)
    }

    @Test
    internal fun getAllBalancesReturnsOnlyMutableResources() {
        val returnResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/quotas/_getAllBalances?folderId=" + TestFolders.TEST_FOLDER_5_ID + "&accountId=" + TestAccounts.TEST_ACCOUNT_7_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontProvisionsDto::class.java)
            .returnResult()

        val provisionsByResourceId = returnResult.responseBody!!.provisionsByResourceId
        assertEquals(1, provisionsByResourceId.size)
        assertFalse(provisionsByResourceId.containsKey(TestResources.YP_HDD_READ_ONLY))
        assertFalse(provisionsByResourceId.containsKey(TestResources.YP_HDD_UNMANAGED))
        assertFalse(provisionsByResourceId.containsKey(TestResources.YP_HDD_UNMANAGED_AND_READ_ONLY))
    }

    @Test
    internal fun getAllBalancesDoesNotReturnCompletelyZeroQuotas() {
        val returnResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/quotas/_getAllBalances?folderId=" + TestFolders.TEST_FOLDER_7_ID + "&accountId=" + TestAccounts.TEST_ACCOUNT_8_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontProvisionsDto::class.java)
            .returnResult()

        val provisionsByResourceId = returnResult.responseBody!!.provisionsByResourceId
        assertEquals(1, provisionsByResourceId.size)
        assertTrue(provisionsByResourceId.containsKey(TestResources.YP_HDD_MAN))
    }

    /**
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController.getAllNonAllocated
     */
    @Test
    internal fun getAllAllocatedTest() {
        val response = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/quotas/_getAllNonAllocated?folderId=" + TestFolders.TEST_FOLDER_1_ID + "&accountId=" + TestAccounts.TEST_ACCOUNT_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontProvisionsDto::class.java)
            .returnResult()
            .responseBody!!

        val provisionsByResourceId = response.provisionsByResourceId
        assertEquals(1, provisionsByResourceId.size)
        assertTrue(provisionsByResourceId.containsKey(TestResources.YP_HDD_MAN))

        // completely zero quotas should not be returned!
        assertFalse(provisionsByResourceId.containsKey(TestResources.YP_CPU_MAN))

        val ypHddManProvisions = provisionsByResourceId[TestResources.YP_HDD_MAN]!!
        assertEquals(valueOf(900), ypHddManProvisions.balance)
        assertEquals(valueOf(100), ypHddManProvisions.providedAbsolute)
        assertEquals(valueOf(0.1), ypHddManProvisions.providedRatio)
        assertEquals(valueOf(100), ypHddManProvisions.allocated)
        assertEquals(valueOf(0.1), ypHddManProvisions.allocatedRatio)
        assertEquals(UnitIds.GIGABYTES, ypHddManProvisions.forEditUnitId)
        assertEquals(valueOf(-100), ypHddManProvisions.providedDelta)
        assertEquals(valueOf(100), ypHddManProvisions.providedAbsoluteInMinAllowedUnit)
        assertEquals(UnitIds.GIGABYTES, ypHddManProvisions.minAllowedUnitId)
        assertEquals(AmountDto("900", "GB",
            "900000000000", "B",
            "900", UnitIds.GIGABYTES,
            "900", UnitIds.GIGABYTES), ypHddManProvisions.balanceAmount)
        assertEquals(AmountDto("-100", "GB",
            "-100000000000", "B",
            "-100", UnitIds.GIGABYTES,
            "-100", UnitIds.GIGABYTES), ypHddManProvisions.deltaAmount)
        assertEquals(AmountDto("100", "GB",
            "100000000000", "B",
            "100", UnitIds.GIGABYTES,
            "100", UnitIds.GIGABYTES), ypHddManProvisions.providedAmount)
    }

    @Test
    internal fun getAllNonAllocatedReturnsOnlyMutableResources() {
        val response = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/quotas/_getAllNonAllocated?folderId=" + TestFolders.TEST_FOLDER_5_ID + "&accountId=" + TestAccounts.TEST_ACCOUNT_7_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontProvisionsDto::class.java)
            .returnResult()
            .responseBody!!

        val provisionsByResourceId = response.provisionsByResourceId
        assertEquals(1, provisionsByResourceId.size)
        assertFalse(provisionsByResourceId.containsKey(TestResources.YP_HDD_READ_ONLY))
        assertFalse(provisionsByResourceId.containsKey(TestResources.YP_HDD_UNMANAGED))
        assertFalse(provisionsByResourceId.containsKey(TestResources.YP_HDD_UNMANAGED_AND_READ_ONLY))
    }

    private fun valueOf(l: Long): String = FrontStringUtil.toString(BigDecimal.valueOf(l))
    private fun valueOf(d: Double): String = FrontStringUtil.toString(BigDecimal.valueOf(d))
}
