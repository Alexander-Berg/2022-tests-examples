package ru.yandex.intranet.d.web.front.history;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestAccounts;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestServices;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.model.folders.FolderHistoryFieldsModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.AbcServiceDto;
import ru.yandex.intranet.d.web.model.AccountDto;
import ru.yandex.intranet.d.web.model.AmountDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.ResourceDto;
import ru.yandex.intranet.d.web.model.UserDto;
import ru.yandex.intranet.d.web.model.folders.front.history.FrontFolderOperationLogDto;
import ru.yandex.intranet.d.web.model.folders.front.history.FrontFolderOperationLogPageDto;
import ru.yandex.intranet.d.web.model.quotas.ProvisionLiteDto;
import ru.yandex.intranet.d.web.model.resources.AccountsSpaceDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestFolderOperations.TEST_FOLDER_OPERATION_LOG_1;
import static ru.yandex.intranet.d.TestFolderOperations.TEST_FOLDER_OPERATION_LOG_NEW;
import static ru.yandex.intranet.d.TestFolderOperations.TEST_FOLDER_OPERATION_LOG_NEW_2;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_SSD_MAN;
import static ru.yandex.intranet.d.UnitIds.BYTES;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.getBody;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.prepareUpdateProvisionsOkResponseTest;
import static ru.yandex.intranet.d.web.model.SortOrderDto.ASC;
import static ru.yandex.intranet.d.web.model.SortOrderDto.DESC;

/**
 * Front folder history API test
 *
 * @author Denis Blokhin <denblo@yandex-team.ru>
 */
@IntegrationTest
public class FrontFolderHistoryApiTest {
    @Autowired
    private WebTestClient webClient;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private AccountsDao accountsDao;

    private static final ParameterizedTypeReference<FrontFolderOperationLogPageDto> PAGE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Test
    public void folderHistoryCanBeFetchedTest() {
        final FrontFolderOperationLogPageDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertTrue(result.getPage().getNextPageToken().isEmpty());
        assertFalse(result.getPage().getItems().isEmpty());
        FrontFolderOperationLogDto firstItem = result.getPage().getItems().get(0);
        assertEquals(FolderOperationType.QUOTA_TRANSFER,
                firstItem.getOperationType());

        final UserDto userDto = result.getUserById().get(TestUsers.USER_1_ID);
        assertEquals("Ivan", userDto.getFirstName());
        assertEquals("Ivanov", userDto.getLastName());
        assertEquals("F", userDto.getGender());

        final ResourceDto resourceDto = result.getResourcesById().get(TestResources.YDB_RAM_SAS);
        assertEquals("YDB-RAM-SAS", resourceDto.getDisplayName());
        assertEquals("Resource RAM for YDB in SAS", resourceDto.getSpecification());

        final AccountDto accountDto = result.getAccountsById().get(TestAccounts.TEST_ACCOUNT_1_ID);
        assertEquals("тестовый аккаунт", accountDto.getDisplayName());
        assertEquals(TestProviders.YP_ID, accountDto.getProviderId());

        assertNotNull(accountDto.getAccountsSpacesId());

        AccountsSpaceDto accountsSpaceDto = result.getAccountsSpacesById().get(accountDto.getAccountsSpacesId());
        assertEquals("MAN default", accountsSpaceDto.getName());

        AmountDto provision = firstItem.getNewProvisions()
                .getProvisionByByAccountId().get(TestAccounts.TEST_ACCOUNT_1_ID)
                .getProvisionByResourceId().get(TestResources.YP_HDD_MAN).getProvision();

        assertEquals(new BigDecimal(provision.getRawAmount()).intValue(), 8);
        assertEquals(new BigDecimal(provision.getReadableAmount()).intValue(), 0);
        assertEquals(provision.getRawUnit(), "B");
        assertEquals(provision.getReadableUnit(), "GB");

        AmountDto quota = firstItem.getNewQuotas().getAmountByResourceId().get(TestResources.YP_HDD_MAN);

        assertEquals(new BigDecimal(quota.getRawAmount()).intValue(), 10);
        assertEquals(new BigDecimal(quota.getReadableAmount()).intValue(), 0);
        assertEquals(quota.getRawUnit(), "B");
        assertEquals(quota.getReadableUnit(), "GB");
    }

    @Test
    public void folderHistoryCanContainsAbcServiceInfoTest() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.upsertOneRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TEST_FOLDER_OPERATION_LOG_NEW.copyBuilder()
                                .setOldFolderFields(FolderHistoryFieldsModel.builder()
                                        .serviceId(TestServices.TEST_SERVICE_ID_DISPENSER)
                                        .build())
                                .setNewFolderFields(FolderHistoryFieldsModel.builder()
                                        .serviceId(TestServices.TEST_SERVICE_ID_D)
                                        .build())
                                .build()
                )
        ).block();

        final FrontFolderOperationLogPageDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        AbcServiceDto dispenser = result.getServicesById().get(TestServices.TEST_SERVICE_ID_DISPENSER);
        assertEquals("Dispenser", dispenser.getName());
        AbcServiceDto d = result.getServicesById().get(TestServices.TEST_SERVICE_ID_D);
        assertEquals("D-service", d.getName());
    }

    @Test
    public void folderHistoryShouldBePagedTest() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(TEST_FOLDER_OPERATION_LOG_NEW, TEST_FOLDER_OPERATION_LOG_NEW_2)
                )
        ).block();

        FrontFolderOperationLogPageDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertFalse(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());

        final String page2Token = result.getPage().getNextPageToken().get();

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&pageToken={token}", TestFolders.TEST_FOLDER_1_ID, page2Token)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertFalse(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        final String page3Token = result.getPage().getNextPageToken().get();

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&pageToken={token}", TestFolders.TEST_FOLDER_1_ID, page3Token)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertTrue(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());

    }

    @Test
    public void invalidFolderHistoryShouldReturnNotFoundTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}", "invalid-id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    public void folderHistoryShouldBeSortedTest() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(TEST_FOLDER_OPERATION_LOG_NEW, TEST_FOLDER_OPERATION_LOG_NEW_2)
                )
        ).block();

        FrontFolderOperationLogPageDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertEquals(3, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW_2.getId(), result.getPage().getItems().get(0).getId());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW.getId(), result.getPage().getItems().get(1).getId());
        assertEquals(TEST_FOLDER_OPERATION_LOG_1.getId(), result.getPage().getItems().get(2).getId());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?sortOrder={sortOrder}", TestFolders.TEST_FOLDER_1_ID, DESC)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertEquals(3, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW_2.getId(), result.getPage().getItems().get(0).getId());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW.getId(), result.getPage().getItems().get(1).getId());
        assertEquals(TEST_FOLDER_OPERATION_LOG_1.getId(), result.getPage().getItems().get(2).getId());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?sortOrder={sortOrder}", TestFolders.TEST_FOLDER_1_ID, ASC)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertEquals(3, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_1.getId(), result.getPage().getItems().get(0).getId());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW.getId(), result.getPage().getItems().get(1).getId());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW_2.getId(), result.getPage().getItems().get(2).getId());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void folderHistoryShouldBeSortedByPageTest() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(TEST_FOLDER_OPERATION_LOG_NEW, TEST_FOLDER_OPERATION_LOG_NEW_2)
                )
        ).block();

        FrontFolderOperationLogPageDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertFalse(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW_2.getId(), result.getPage().getItems().get(0).getId());

        String page2Token = result.getPage().getNextPageToken().get();

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&pageToken={token}", TestFolders.TEST_FOLDER_1_ID, page2Token)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertFalse(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW.getId(), result.getPage().getItems().get(0).getId());
        String page3Token = result.getPage().getNextPageToken().get();

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&pageToken={token}", TestFolders.TEST_FOLDER_1_ID, page3Token)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertTrue(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_1.getId(), result.getPage().getItems().get(0).getId());


        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&sortOrder={sortOrder}", TestFolders.TEST_FOLDER_1_ID, DESC)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertFalse(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW_2.getId(), result.getPage().getItems().get(0).getId());

        page2Token = result.getPage().getNextPageToken().get();

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&pageToken={token}&sortOrder={sortOrder}",
                        TestFolders.TEST_FOLDER_1_ID, page2Token, DESC)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertFalse(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW.getId(), result.getPage().getItems().get(0).getId());
        page3Token = result.getPage().getNextPageToken().get();

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&pageToken={token}&sortOrder={sortOrder}",
                        TestFolders.TEST_FOLDER_1_ID, page3Token, DESC)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertTrue(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_1.getId(), result.getPage().getItems().get(0).getId());


        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&sortOrder={sortOrder}", TestFolders.TEST_FOLDER_1_ID, ASC)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertFalse(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_1.getId(), result.getPage().getItems().get(0).getId());

        page2Token = result.getPage().getNextPageToken().get();

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&pageToken={token}&sortOrder={sortOrder}",
                        TestFolders.TEST_FOLDER_1_ID, page2Token, ASC)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertFalse(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW.getId(), result.getPage().getItems().get(0).getId());
        page3Token = result.getPage().getNextPageToken().get();

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}?limit=1&pageToken={token}&sortOrder={sortOrder}",
                        TestFolders.TEST_FOLDER_1_ID, page3Token, ASC)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertTrue(result.getPage().getNextPageToken().isEmpty());
        assertEquals(1, result.getPage().getItems().size());
        assertEquals(TEST_FOLDER_OPERATION_LOG_NEW_2.getId(), result.getPage().getItems().get(0).getId());
    }

    @Test
    public void folderHistoryMustContainDeletedAccountTest() {
        prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.removeRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TEST_ACCOUNT_1_ID, Tenants.DEFAULT_TENANT_ID, TEST_ACCOUNT_1.getVersion()
                )
        ).block();

        FrontFolderOperationLogPageDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getAccountsById());
        Assertions.assertEquals(1, result.getAccountsById().size());
        Assertions.assertEquals(TEST_ACCOUNT_1_ID, result.getAccountsById().values().iterator().next()
                .getId());
        Assertions.assertEquals(TestProviders.YP_ID, result.getAccountsById().values().iterator().next()
                .getProviderId());
    }

    @Test
    public void folderHistoryMustContainDeltasTest() {
        prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        FrontFolderOperationLogPageDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/history/folders/{id}", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PAGE_TYPE)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        PageDto<FrontFolderOperationLogDto> page = result.getPage();
        Assertions.assertNotNull(page);
        FrontFolderOperationLogDto frontFolderOperationLogDto = page.getItems().get(0);
        FrontFolderOperationLogDto.AmountByResourceId balanceDelta = frontFolderOperationLogDto.getBalanceDelta();
        Assertions.assertNotNull(balanceDelta);
        Assertions.assertEquals(new FrontFolderOperationLogDto.AmountByResourceId(
                        Map.of(
                                YP_SSD_MAN,
                                new AmountDto(
                                        "-10",
                                        "GB",
                                        "-10000000000",
                                        "B",
                                        "-10000000000",
                                        BYTES,
                                        "-10000000000",
                                        BYTES
                                ),
                                YP_HDD_MAN,
                                new AmountDto(
                                        "100",
                                        "GB",
                                        "100000000000",
                                        "B",
                                        "100000000000",
                                        BYTES,
                                        "100000000000",
                                        BYTES
                                )
                        )
                ),
                balanceDelta);
        FrontFolderOperationLogDto.ProvisionDeltasByAccountId provisionsDelta =
                frontFolderOperationLogDto.getProvisionsDelta();
        Assertions.assertNotNull(provisionsDelta);
        Assertions.assertEquals(new FrontFolderOperationLogDto.ProvisionDeltasByAccountId(
                        Map.of(TEST_ACCOUNT_1_ID,
                                new FrontFolderOperationLogDto.ProvisionDeltasByResourceId(Map.of(
                                        YP_SSD_MAN,
                                        new AmountDto(
                                                "10",
                                                "GB",
                                                "10000000000",
                                                "B",
                                                "10000000000",
                                                BYTES,
                                                "10000000000",
                                                BYTES
                                        ),
                                        YP_HDD_MAN,
                                        new AmountDto(
                                                "-100",
                                                "GB",
                                                "-100000000000",
                                                "B",
                                                "-100000000000",
                                                BYTES,
                                                "-100000000000",
                                                BYTES
                                        )
                                )
                        ))
                ),
                provisionsDelta);
        FrontFolderOperationLogDto.AmountByResourceId quotasDelta = frontFolderOperationLogDto.getQuotasDelta();
        Assertions.assertNotNull(quotasDelta);
        Assertions.assertEquals(new FrontFolderOperationLogDto.AmountByResourceId(
                        Map.of(
                                YP_SSD_MAN,
                                new AmountDto(
                                        "0",
                                        "B",
                                        "0",
                                        "B",
                                        "0",
                                        BYTES,
                                        "0",
                                        BYTES
                                ),
                                YP_HDD_MAN,
                                new AmountDto(
                                        "0",
                                        "B",
                                        "0",
                                        "B",
                                        "0",
                                        BYTES,
                                        "0",
                                        BYTES
                                )
                        )
                ),
                quotasDelta);
    }

}
