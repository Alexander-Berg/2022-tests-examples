package ru.yandex.intranet.d.web.admin.providers;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestAccounts;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResourceTypes;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao;
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao;
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao;
import ru.yandex.intranet.d.datasource.impl.YdbRetry;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.metrics.YdbMetrics;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.folders.FolderOperationType;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel;
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel;
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;
import ru.yandex.intranet.d.services.admin.JobState;
import ru.yandex.intranet.d.services.admin.JobStatus;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.FeatureStateDto;
import ru.yandex.intranet.d.web.model.FeatureStateInputDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.folders.FrontFolderDto;
import ru.yandex.intranet.d.web.model.providers.AggregationAlgorithmInputDto;
import ru.yandex.intranet.d.web.model.providers.AggregationQuotaQueryTypeInputDto;
import ru.yandex.intranet.d.web.model.providers.AggregationSettingsInputDto;
import ru.yandex.intranet.d.web.model.providers.ExternalAccountUrlTemplateDto;
import ru.yandex.intranet.d.web.model.providers.FreeProvisionAggregationModeInputDto;
import ru.yandex.intranet.d.web.model.providers.FullProviderDto;
import ru.yandex.intranet.d.web.model.providers.ProviderCreateDto;
import ru.yandex.intranet.d.web.model.providers.ProviderPutDto;
import ru.yandex.intranet.d.web.model.providers.UsageModeInputDto;
import ru.yandex.intranet.d.web.model.quotas.ClearedQuotaDto;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestUsers.USER_1_UID;

/**
 * Providers admin API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class AdminProvidersApiTest {
    private static final long DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE = 1000L;

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private ProvidersDao providersDao;
    @Autowired
    private ResourceTypesDao resourceTypesDao;
    @Autowired
    private ResourceSegmentationsDao resourceSegmentationsDao;
    @Autowired
    private ResourceSegmentsDao resourceSegmentsDao;
    @Autowired
    private ResourcesDao resourcesDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private YdbMetrics ydbMetrics;
    @Value("${ydb.transactionRetries}")
    private long transactionRetries;

    public static AccountsQuotasModel accountsQuotasModel(String providerId, String resourceId, String folderId,
                                                          String accountId, long allocatedQuota, long providedQuota) {
        return new AccountsQuotasModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setAccountId(accountId)
                .setResourceId(resourceId)
                .setProviderId(providerId)
                .setAllocatedQuota(allocatedQuota)
                .setFolderId(folderId)
                .setProvidedQuota(providedQuota)
                .setLastProvisionUpdate(Instant.now())
                .setLastReceivedProvisionVersion(1L)
                .setLatestSuccessfulProvisionOperationId(UUID.randomUUID().toString())
                .build();
    }

    @Test
    public void getProviderTest() {
        FullProviderDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .get()
                .uri("/admin/providers/{id}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", result.getId());
    }

    @Test
    public void getProviderNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .get()
                .uri("/admin/providers/{id}", "12345678-9012-3456-7890-123456789012")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void getProvidersPageTest() {
        PageDto<FullProviderDto> result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .get()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FullProviderDto>>() { })
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getProvidersTwoPagesTest() {
        PageDto<FullProviderDto> firstResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .get()
                .uri("/admin/providers?limit={limit}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FullProviderDto>>() { })
                .returnResult()
                .getResponseBody();
        assertNotNull(firstResult);
        assertEquals(1, firstResult.getItems().size());
        assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<FullProviderDto> secondResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .get()
                .uri("/admin/providers?limit={limit}&pageToken={token}",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FullProviderDto>>() { })
                .returnResult()
                .getResponseBody();
        assertNotNull(secondResult);
        assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void createProviderTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        300L),
                new AggregationAlgorithmInputDto(false, 1000L, AggregationQuotaQueryTypeInputDto.PAGINATE,
                        false, true, 3L, true, false), null,
                List.of(new ExternalAccountUrlTemplateDto(Map.of(), true, Map.of(), true)),
                FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getReserveFolderId());
        Optional<FolderModel> reserveFolder = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderDao.getById(txSession, result.getReserveFolderId(), Tenants.DEFAULT_TENANT_ID))).block();
        assertNotNull(reserveFolder);
        assertTrue(reserveFolder.isPresent());
        assertNotNull(result.getExternalAccountUrlTemplates());
        assertTrue(result.getExternalAccountUrlTemplates().isPresent());
        assertEquals(FolderType.PROVIDER_RESERVE, reserveFolder.get().getFolderType());
    }

    @Test
    public void createProviderAllocatedSupportedTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.ENABLED,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        300L),
                new AggregationAlgorithmInputDto(false, 1000L, AggregationQuotaQueryTypeInputDto.PAGINATE,
                        false, true, 3L, true, false), null, emptyList(),
                FeatureStateInputDto.ENABLED);
        FullProviderDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getReserveFolderId());
        Optional<FolderModel> reserveFolder = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderDao.getById(txSession, result.getReserveFolderId(), Tenants.DEFAULT_TENANT_ID))).block();
        assertNotNull(reserveFolder);
        assertTrue(reserveFolder.isPresent());
        assertEquals(FolderType.PROVIDER_RESERVE, reserveFolder.get().getFolderType());
        assertEquals(FeatureStateDto.ENABLED, result.getAllocatedSupported());
    }

    @Test
    public void createProviderNonDuplicateReserveFolderTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(TestFolders.TEST_FOLDER_1_RESERVE_ID, result.getReserveFolderId());
    }

    @Test
    public void createProviderStaticGrpcUriTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "static://localhost:9090,localhost:9091",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertNotNull(result.getId());
    }

    @Test
    public void createProviderKeyConflictTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "yp",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        300L),
                new AggregationAlgorithmInputDto(false, 1000L, AggregationQuotaQueryTypeInputDto.PAGINATE,
                        false, true, 3L, true, false), null, emptyList(),
                FeatureStateInputDto.UNSPECIFIED);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    public void createProviderMissingServiceTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 999L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    public void updateProviderTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto createResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(createResult);
        assertNotNull(createResult.getId());
        ProviderPutDto putDto = new ProviderPutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", "https://localhost:8081", "dns://localhost:9091",
                3L, 4L, 2L, true, false, true, false,
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        null),
                new AggregationAlgorithmInputDto(false, 1000L, AggregationQuotaQueryTypeInputDto.PAGINATE,
                        false, true, 3L, true, false), null, emptyList(),
                FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto updateResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .put()
                .uri("/admin/providers/{id}?version={version}", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(updateResult);
        assertNotNull(updateResult.getId());
        assertEquals(createResult.getVersion() + 1, updateResult.getVersion());
    }

    @Test
    public void updateProviderAllocatedSupportedTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED,
                new AggregationSettingsInputDto(FreeProvisionAggregationModeInputDto.NONE, UsageModeInputDto.UNDEFINED,
                        null),
                new AggregationAlgorithmInputDto(false, 1000L, AggregationQuotaQueryTypeInputDto.PAGINATE,
                        false, true, 3L, true, false), null, emptyList(),
                FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto createResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(createResult);
        assertNotNull(createResult.getId());
        ProviderPutDto putDto = new ProviderPutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", "https://localhost:8081", "dns://localhost:9091",
                3L, 4L, 2L, true, false, true, false,
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.ENABLED, null,
                null, null, emptyList(),
                FeatureStateInputDto.ENABLED);
        FullProviderDto updateResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .put()
                .uri("/admin/providers/{id}?version={version}", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(updateResult);
        assertNotNull(updateResult.getId());
        assertEquals(createResult.getVersion() + 1, updateResult.getVersion());
        assertEquals(FeatureStateDto.ENABLED, updateResult.getAllocatedSupported());
    }

    @Test
    public void updateProviderNotFoundTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto createResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(createResult);
        assertNotNull(createResult.getId());
        ProviderPutDto putDto = new ProviderPutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", "https://localhost:8081", "dns://localhost:9091",
                3L, 4L, 2L, true, false, true, false,
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .put()
                .uri("/admin/providers/{id}?version={version}", "12345678-9012-3456-7890-123456789012",
                        createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(updateResult);
        assertFalse(updateResult.getErrors().isEmpty());
    }

    @Test
    public void updateProviderVersionMismatchTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto createResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(createResult);
        assertNotNull(createResult.getId());
        ProviderPutDto putDto = new ProviderPutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", "https://localhost:8081", "dns://localhost:9091",
                3L, 4L, 2L, true, false, true, false,
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .put()
                .uri("/admin/providers/{id}?version={version}", createResult.getId(), createResult.getVersion() + 1L)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.PRECONDITION_FAILED)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(updateResult);
        assertFalse(updateResult.getFieldErrors().isEmpty());
    }

    @Test
    public void updateProviderInvalidServiceTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto createResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(createResult);
        assertNotNull(createResult.getId());
        ProviderPutDto putDto = new ProviderPutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", "https://localhost:8081", "dns://localhost:9091",
                3L, 4L, 999L, true, false, true, false,
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        ErrorCollectionDto updateResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .put()
                .uri("/admin/providers/{id}?version={version}", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(updateResult);
        assertFalse(updateResult.getFieldErrors().isEmpty());
    }

    @Test
    public void updateToManagedProviderTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, true, true, false, false, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto createResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(createResult);
        assertNotNull(createResult.getId());
        assertNull(createResult.getReserveFolderId());

        ProviderPutDto putDto = new ProviderPutDto("Test-1", "Тест-1", "Test description-1",
                "Тестовое описание-1", "https://localhost:8081", "dns://localhost:9091",
                3L, 4L, 2L, false, false, true, true,
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto updateResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .put()
                .uri("/admin/providers/{id}?version={version}", createResult.getId(), createResult.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(updateResult);
        assertNotNull(updateResult.getId());
        assertNotNull(updateResult.getReserveFolderId());
        Optional<FolderModel> reserveFolder = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderDao.getById(txSession, updateResult.getReserveFolderId(), Tenants.DEFAULT_TENANT_ID)))
                .block();
        assertNotNull(reserveFolder);
        assertTrue(reserveFolder.isPresent());
        assertEquals(FolderType.PROVIDER_RESERVE, reserveFolder.get().getFolderType());
    }

    @Test
    public void createProviderAcceptableForDAdminsTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertNotNull(result.getId());
    }

    @Test
    public void createProviderForbiddenForNonDAdminsTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.NOT_D_ADMIN_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void cleanQuotasTest() {
        ydbTableClient.usingSessionMonoRetryable(session -> accountsQuotasDao.upsertAllRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(
                        new AccountsQuotasModel.Builder()
                                .setResourceId(TestResources.YP_SSD_VLA)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setAccountId(TestAccounts.TEST_ACCOUNT_1_ID)
                                .setFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .setProviderId(YP_ID)
                                .setProvidedQuota(0L)
                                .setAllocatedQuota(0L)
                                .setLastProvisionUpdate(Instant.now())
                                .build()
                        )
                )
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        List<QuotaModel> quotasBeforeDeleteSSD = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByResourceIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestResources.YP_SSD_VLA,
                                TestResources.YP_SSD_MAN),
                        100)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        assertNotNull(quotasBeforeDeleteSSD);
        assertFalse(quotasBeforeDeleteSSD.isEmpty());

        List<QuotaModel> quotasBeforeDeleteHDD = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByResourceId(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        TestResources.YP_HDD_MAN,
                        100)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        assertNotNull(quotasBeforeDeleteHDD);
        assertFalse(quotasBeforeDeleteHDD.isEmpty());

        List<AccountsQuotasModel> accountsQuotasBeforeDelete = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getAllByTenant(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        assertNotNull(accountsQuotasBeforeDelete);
        long countClearingAccountQuotas = accountsQuotasBeforeDelete.stream()
                .map(AccountsQuotasModel::getResourceId)
                .filter(resourceId ->
                        resourceId.equals(TestResources.YP_SSD_MAN) || resourceId.equals(TestResources.YP_SSD_VLA))
                .count();

        List<FolderOperationLogModel> folderOperationLogBeforeUpsert = ydbTableClient.usingSessionMonoRetryable(
                session ->
                        folderOperationLogDao.getFirstPageByFolder(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                                Tenants.DEFAULT_TENANT_ID, TestFolders.TEST_FOLDER_2_ID, SortOrderDto.ASC, 100)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        assertNotNull(folderOperationLogBeforeUpsert);

        List<FolderOperationLogModel> folderOperationLogBeforeUpsert2 = ydbTableClient.usingSessionMonoRetryable(
                session ->
                        folderOperationLogDao.getFirstPageByFolder(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                                Tenants.DEFAULT_TENANT_ID, TestFolders.TEST_FOLDER_3_ID, SortOrderDto.ASC, 100)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                .block();
        assertNotNull(folderOperationLogBeforeUpsert2);

        ClearedQuotaDto response = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .delete()
                .uri("/admin/providers/quotas/{providerId}?resourceTypeId={resourceTypeId}",
                        YP_ID,
                        TestResourceTypes.YP_SSD
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClearedQuotaDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(response);
        String jobId = response.getJobId();

        JobStatus jobStatus;
        do {
            jobStatus = webClient
                    .mutateWith(MockUser.uid(USER_1_UID))
                    .get()
                    .uri("/admin/providers/job/{jobId}", jobId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(JobStatus.class)
                    .returnResult()
                    .getResponseBody();
        } while (jobStatus != null && jobStatus.getState() == JobState.RUNNING);

        assertNotNull(jobStatus);
        assertEquals(
                String.format("ClearedQuotaDto(countClearedQuotas=%1$d, countClearedAccountQuotas=%2$d, jobId=null)",
                        quotasBeforeDeleteSSD.size(), countClearingAccountQuotas
                ),
                jobStatus.getMessage()
        );

        List<QuotaModel> quotasNotDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByResourceId(session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TestResources.YP_HDD_MAN, 100)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        assertNotNull(quotasNotDeleted);
        assertEquals(quotasBeforeDeleteHDD.size(), quotasNotDeleted.size());

        List<QuotaModel> quotasDeleted = ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.getByResourceIds(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestResources.YP_SSD_VLA,
                                TestResources.YP_SSD_MAN),
                        100)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        assertNotNull(quotasDeleted);
        assertTrue(quotasDeleted.isEmpty());

        List<AccountsQuotasModel> accountsQuotasAfterDelete = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getAllByTenant(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        assertNotNull(accountsQuotasAfterDelete);
        assertEquals(accountsQuotasBeforeDelete.size() - countClearingAccountQuotas,
                accountsQuotasAfterDelete.size());

        List<FolderOperationLogModel> folderOperationLogAfterUpsertWithAccountQuotas =
                ydbTableClient.usingSessionMonoRetryable(session ->
                        folderOperationLogDao.getFirstPageByFolder(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                                Tenants.DEFAULT_TENANT_ID, TestFolders.TEST_FOLDER_2_ID, SortOrderDto.ASC, 100)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                        .block();
        assertNotNull(folderOperationLogAfterUpsertWithAccountQuotas);
        assertEquals(folderOperationLogBeforeUpsert.size() + 1,
                folderOperationLogAfterUpsertWithAccountQuotas.size());
        Optional<FolderOperationLogModel> cleanLog = folderOperationLogAfterUpsertWithAccountQuotas.stream()
                .filter(log -> log.getOperationType() == FolderOperationType.CLEAN_QUOTAS_BY_ADMIN)
                .findFirst();
        assertTrue(cleanLog.isPresent());
        cleanLog.ifPresent(log -> {
            assertEquals(0L, log.getNewQuotas().get(TestResources.YP_SSD_VLA));
            assertEquals(0L, log.getNewBalance().get(TestResources.YP_SSD_VLA));
            assertEquals(1L, log.getNewProvisions().size());
            assertEquals(0L, log.getNewProvisions().get(TestAccounts.TEST_ACCOUNT_1_ID)
                            .get(TestResources.YP_SSD_VLA).getProvision());
        });

        List<FolderOperationLogModel> folderOperationLogAfterUpsertWithoutAccountQuotas =
                ydbTableClient.usingSessionMonoRetryable(session ->
                        folderOperationLogDao.getFirstPageByFolder(session.asTxCommit(TransactionMode.ONLINE_READ_ONLY),
                                Tenants.DEFAULT_TENANT_ID, TestFolders.TEST_FOLDER_3_ID, SortOrderDto.ASC, 100)
                                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics)))
                        .block();
        assertNotNull(folderOperationLogAfterUpsertWithoutAccountQuotas);
        assertEquals(folderOperationLogBeforeUpsert2.size() + 1,
                folderOperationLogAfterUpsertWithoutAccountQuotas.size());
        assertTrue(folderOperationLogAfterUpsertWithoutAccountQuotas.stream()
                .filter(log -> log.getOperationType() == FolderOperationType.CLEAN_QUOTAS_BY_ADMIN).anyMatch(log ->
                        log.getNewQuotas().get(TestResources.YP_SSD_VLA) == 0L &&
                                log.getNewBalance().get(TestResources.YP_SSD_VLA) == 0L &&
                                log.getOldProvisions().size() == 0));
    }

    @Test
    public void cleanQuotasFolderQuotaNotFoundTest() {
        ClearedQuotaDto response = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .delete()
                .uri("/admin/providers/quotas/{providerId}",
                        YP_ID
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClearedQuotaDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(response);
        String jobId = response.getJobId();

        JobStatus jobStatus;
        do {
            jobStatus = webClient
                    .mutateWith(MockUser.uid(USER_1_UID))
                    .get()
                    .uri("/admin/providers/job/{jobId}", jobId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(JobStatus.class)
                    .returnResult()
                    .getResponseBody();
        } while (jobStatus != null && jobStatus.getState() == JobState.RUNNING);

        assertNotNull(jobStatus);
        assertEquals(JobState.FAILURE, jobStatus.getState());
        assertNotNull(jobStatus.getMessage());
        assertTrue(jobStatus.getMessage().contains("This account quota has no folder quota"));
    }

    @Test
    public void cleanQuotasFolderQuotaNotFoundWithZeroProvidedTest() {
        AccountsQuotasModel.Identity accountQuotaId = new AccountsQuotasModel.Identity(
                "56a41608-84df-41c4-9653-89106462e0ce", "14e2705c-ff49-43a4-8048-622e373f5891");
        Optional<AccountsQuotasModel> accountsQuotasOptional = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.getById(session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                        accountQuotaId, Tenants.DEFAULT_TENANT_ID)).block();
        assertNotNull(accountsQuotasOptional);
        assertTrue(accountsQuotasOptional.isPresent());
        AccountsQuotasModel accountsQuotas = accountsQuotasOptional.get();
        AccountsQuotasModel newAccountsQuotas = new AccountsQuotasModel.Builder(accountsQuotas)
                .setProvidedQuota(0L)
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsQuotasDao.upsertOneRetryable(session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                        newAccountsQuotas)).block();

        ClearedQuotaDto response = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .delete()
                .uri("/admin/providers/quotas/{providerId}",
                        YP_ID
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClearedQuotaDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(response);
        String jobId = response.getJobId();

        JobStatus jobStatus;
        do {
            jobStatus = webClient
                    .mutateWith(MockUser.uid(USER_1_UID))
                    .get()
                    .uri("/admin/providers/job/{jobId}", jobId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(JobStatus.class)
                    .returnResult()
                    .getResponseBody();
        } while (jobStatus != null && jobStatus.getState() == JobState.RUNNING);

        assertNotNull(jobStatus);
        assertEquals(JobState.SUCCESS, jobStatus.getState());
    }

    @Test
    public void cleanQuotasProviderNotFoundTest() {
        ErrorCollectionDto resultNotFoundResourceType = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .delete()
                .uri("/admin/providers/quotas/{providerId}",
                        "notValidUid"
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(resultNotFoundResourceType);
        assertFalse(resultNotFoundResourceType.getErrors().isEmpty());
        assertEquals(
                "Provider not found.",
                resultNotFoundResourceType.getErrors().stream().findFirst().get());
    }

    @Test
    public void cleanQuotasNotFoundTest() {
        ClearedQuotaDto response = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .delete()
                .uri("/admin/providers/quotas/{providerId}?resourceTypeId={resourceTypeId}",
                        YP_ID,
                        "notValidUid"
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClearedQuotaDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(response);
        String jobId = response.getJobId();

        JobStatus jobStatus;
        do {
            jobStatus = webClient
                    .mutateWith(MockUser.uid(USER_1_UID))
                    .get()
                    .uri("/admin/providers/job/{jobId}", jobId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(JobStatus.class)
                    .returnResult()
                    .getResponseBody();
        } while (jobStatus != null && jobStatus.getState() == JobState.RUNNING);

        assertNotNull(jobStatus);
        assertEquals(JobState.FAILURE, jobStatus.getState());
        assertNotNull(jobStatus.getMessage());
        assertTrue(jobStatus.getMessage().contains("Resource type not found"));
    }

    @Test
    public void cleanQuotasForbiddenTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.NOT_D_ADMIN_UID))
                .delete()
                .uri("/admin/providers/quotas/{providerId}?resourceTypeId={resourceTypeId}",
                        YP_ID,
                        TestResourceTypes.YP_SSD
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void createReserveFolderTest() {
        FrontFolderDto result = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers/{id}/reserveFolder", TestProviders.CLAUD1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertNotNull(result.getId());
        Optional<FolderModel> reserveFolder = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        folderDao.getById(txSession, result.getId(), Tenants.DEFAULT_TENANT_ID))).block();
        assertNotNull(reserveFolder);
        assertTrue(reserveFolder.isPresent());
        assertEquals(FolderType.PROVIDER_RESERVE, result.getFolderType());
        Optional<ProviderModel> provider = tableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        providersDao.getById(txSession, TestProviders.CLAUD1_ID, Tenants.DEFAULT_TENANT_ID))).block();
        assertNotNull(provider);
        assertTrue(provider.isPresent());
        assertNotNull(provider.get().getReserveFolderId());
        assertTrue(provider.get().getReserveFolderId().isPresent());
        assertEquals(reserveFolder.get().getId(), provider.get().getReserveFolderId().get());
    }

    @Test
    public void createReserveFolderInNonManagedProviderTest() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, true, true, false, false, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, emptyList(), FeatureStateInputDto.UNSPECIFIED);
        FullProviderDto createResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullProviderDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(createResult);
        assertNotNull(createResult.getId());
        assertNull(createResult.getReserveFolderId());

        ErrorCollectionDto reserveFolderResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers/{id}/reserveFolder", createResult.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(reserveFolderResult);
        assertNotNull(reserveFolderResult.getErrors());
        assertTrue(reserveFolderResult.getErrors().contains("Provider is non managed."));
    }

    @Test
    public void createReserveFolderAlreadyExistsTest() {
        ErrorCollectionDto reserveFolderResult = webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers/{id}/reserveFolder", TestProviders.YDB_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(reserveFolderResult);
        assertNotNull(reserveFolderResult.getErrors());
        assertTrue(reserveFolderResult.getErrors().contains("Reserve folder already exists."));
    }

    @Test
    public void createProviderWithExternalAccountUrlTemplatesValidation() {
        ProviderCreateDto createDto = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, List.of(new ExternalAccountUrlTemplateDto(null, null, null, null)),
                FeatureStateInputDto.UNSPECIFIED);
        webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result.getResponseBody());
                    Assertions.assertNotNull(result.getResponseBody().getFieldErrors());
                    Map<String, Set<String>> fieldErrors = result.getResponseBody().getFieldErrors();
                    Assertions.assertEquals(Set.of("Field is required."),
                            fieldErrors.get("externalAccountUrlTemplates.0.urlsForSegments"));
                    Assertions.assertEquals(Set.of("Field is required."),
                            fieldErrors.get("externalAccountUrlTemplates.0.segments"));
                    Assertions.assertEquals(Set.of("Field is required."),
                            fieldErrors.get("externalAccountUrlTemplates.0.urlTemplates"));
                    Assertions.assertEquals(Set.of("Field is required."),
                            fieldErrors.get("externalAccountUrlTemplates.0.defaultTemplate"));
                });

        HashMap<String, Set<String>> segments = new HashMap<>();
        segments.put("segmentation", null);
        HashMap<String, String> urlTemplates = new HashMap<>();
        urlTemplates.put("name", null);
        var listExternalAccountUrlTemplateDto = List.of(new ExternalAccountUrlTemplateDto(
                segments,
                true,
                urlTemplates,
                true));
        ProviderCreateDto createDto2 = new ProviderCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", "https://localhost:8080", "dns://localhost:9090",
                1L, 2L, 1L, false, true, false, true, "test",
                true, true, true, true, true, true, true, true, true, true, true, true, true, false, false,
                DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, true, true, null, 1L, null, FeatureStateInputDto.UNSPECIFIED, null,
                null, null, listExternalAccountUrlTemplateDto, FeatureStateInputDto.UNSPECIFIED);
        webClient
                .mutateWith(MockUser.uid(USER_1_UID))
                .post()
                .uri("/admin/providers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto2)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result.getResponseBody());
                    Assertions.assertNotNull(result.getResponseBody().getFieldErrors());
                    Map<String, Set<String>> fieldErrors = result.getResponseBody().getFieldErrors();
                    Assertions.assertNull(fieldErrors.get("externalAccountUrlTemplates.0.urlTemplates"));
                    Assertions.assertNull(fieldErrors.get("externalAccountUrlTemplates.0.defaultTemplate"));
                    Assertions.assertEquals(Set.of("Field is required."),
                            fieldErrors.get("externalAccountUrlTemplates.0.segments.segmentation"));
                    Assertions.assertEquals(Set.of("Field is required."),
                            fieldErrors.get("externalAccountUrlTemplates.0.urlTemplates.name"));
                });
    }

    public static ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported) {
        return providerModel(grpcUri, restUri, accountsSpacesSupported, 69L);
    }

    public static ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported,
                                       long serviceId) {
        return providerModelBuilder(grpcUri, restUri, accountsSpacesSupported, serviceId).build();
    }

    static ProviderModel.Builder providerModelBuilder(String grpcUri, String restUri, boolean accountsSpacesSupported,
                                                      long serviceId) {
        return ProviderModel.builder()
                .id(UUID.randomUUID().toString())
                .grpcApiUri(grpcUri)
                .restApiUri(restUri)
                .destinationTvmId(42L)
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .sourceTvmId(42L)
                .serviceId(serviceId)
                .deleted(false)
                .readOnly(false)
                .multipleAccountsPerFolder(true)
                .accountTransferWithQuota(true)
                .managed(true)
                .key("test")
                .trackerComponentId(1L)
                .accountsSettings(AccountsSettingsModel.builder()
                        .displayNameSupported(true)
                        .keySupported(true)
                        .deleteSupported(true)
                        .softDeleteSupported(true)
                        .moveSupported(true)
                        .renameSupported(true)
                        .perAccountVersionSupported(true)
                        .perProvisionVersionSupported(true)
                        .perAccountLastUpdateSupported(true)
                        .perProvisionLastUpdateSupported(true)
                        .operationIdDeduplicationSupported(true)
                        .syncCoolDownDisabled(false)
                        .retryCoolDownDisabled(false)
                        .accountsSyncPageSize(1000L)
                        .build())
                .importAllowed(true)
                .accountsSpacesSupported(accountsSpacesSupported).syncEnabled(true).grpcTlsOn(true);
    }

    public static ResourceTypeModel resourceTypeModel(String providerId, String key, String unitsEnsembleId) {
        return ResourceTypeModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId(unitsEnsembleId)
                .build();
    }

    public static ResourceSegmentationModel resourceSegmentationModel(String providerId, String key) {
        return ResourceSegmentationModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .build();
    }

    public static ResourceSegmentModel resourceSegmentModel(String segmentationId, String key) {
        return ResourceSegmentModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .segmentationId(segmentationId)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .build();
    }

    @SuppressWarnings("ParameterNumber")
    public static ResourceModel resourceModel(String providerId, String key, String resourceTypeId,
                                              Set<Tuple2<String, String>> segments, String unitsEnsembleId,
                                              Set<String> allowedUnitIds, String defaultUnitId,
                                              String baseUnitId, String accountsSpaceId) {
        return ResourceModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId(unitsEnsembleId)
                .providerId(providerId)
                .resourceTypeId(resourceTypeId)
                .segments(segments.stream().map(t -> new ResourceSegmentSettingsModel(t.getT1(), t.getT2()))
                        .collect(Collectors.toSet()))
                .resourceUnits(new ResourceUnitsModel(allowedUnitIds, defaultUnitId, null))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId(baseUnitId)
                .accountsSpacesId(accountsSpaceId)
                .build();
    }

    public static FolderModel folderModel(long serviceId) {
        return FolderModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setServiceId(serviceId)
                .setVersion(0L)
                .setDisplayName("Test")
                .setDescription("Test")
                .setDeleted(false)
                .setFolderType(FolderType.COMMON)
                .setTags(Collections.emptySet())
                .setNextOpLogOrder(1L)
                .build();
    }

    public static QuotaModel quotaModel(String providerId, String resourceId, String folderId, long quota,
                                        long balance) {
        return QuotaModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .resourceId(resourceId)
                .folderId(folderId)
                .quota(quota)
                .balance(balance)
                .frozenQuota(0L)
                .build();
    }

}
