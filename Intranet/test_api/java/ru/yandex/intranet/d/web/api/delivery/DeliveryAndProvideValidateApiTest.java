package ru.yandex.intranet.d.web.api.delivery;

import java.util.Set;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestAccounts;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.delivery.DeliverableDeltaDto;
import ru.yandex.intranet.d.web.model.delivery.DeliverableMetaRequestDto;
import ru.yandex.intranet.d.web.model.delivery.provide.DeliveryAndProvideDestinationDto;
import ru.yandex.intranet.d.web.model.delivery.provide.DeliveryAndProvideRequestDto;

import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_HDD_READ_ONLY;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_UID;

/**
 * Delivery and provide API test (validation errors).
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
public class DeliveryAndProvideValidateApiTest {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private ProvidersDao providersDao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Value("${hardwareOrderService.tvmSourceId}")
    private long dispenserTvmSourceId;

    @Test
    public void testNotFoundAuthorUid() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("0000000000000")
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("authorUid");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("User not found."));
                });
    }

    @Test
    public void testDeliveryToNotExistsService() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(54321L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.serviceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Service not found."));
                });
    }

    @Test
    public void testDeliveryToNotExistsProvider() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId("00000000-0000-0000-ab09-6830cf1fee50")
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.providerId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Provider not found."));
                });
    }

    @Test
    public void testDeliveryROProvider() {
        ydbTableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE, ts ->
                                providersDao.getById(ts, YP_ID, Tenants.DEFAULT_TENANT_ID)
                                        .flatMap(yp -> providersDao.updateProviderRetryable(ts,
                                                ProviderModel.builder(yp.orElseThrow())
                                                        .readOnly(true)
                                                        .build()))))
                .block();

        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.providerId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Provider in read only state."));
                });
    }

    @Test
    public void testDeliveryToNotExistsFolder() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId("00000000-0000-0000-ab09-6830cf1fee50")
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.folderId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Folder not found."));
                });
    }

    @Test
    public void testDeliveryToNotExistsAccount() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId("00000000-0000-0000-ab09-6830cf1fee50")
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.accountId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Account not found."));
                });
    }

    @Test
    public void testDeliveryToNotExistsResource() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId("00000000-0000-0000-ab09-6830cf1fee50")
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.resourceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Resource not found."));
                });
    }

    @Test
    public void testDeliveryROResource() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(YP_HDD_READ_ONLY)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.resourceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Resource in read only state."));
                });
    }

    @Test
    public void testDeliveryToNotConsistServiceAndFolder() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_17_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.folderId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Folder not found."));
                });
    }

    @Test
    public void testDeliveryToNotConsistAccountAndFolder() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_17_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.accountId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Account not found."));
                });
    }

    @Test
    public void testDeliveryToNotConsistResourceAndProvider() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YDB_RAM_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.resourceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Resource not found."));
                });
    }

    @Test
    public void testDeliveryToClosingService() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(13L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_SSD_VLA)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.serviceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Current service status is not allowed."));
                });
    }

    @Test
    public void testDeliveryToNonExportableService() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(15L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_SSD_VLA)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(1L)
                                .bigOrderId(42L)
                                .build())
                        .build())
                .build();

        webClient
                .mutateWith(MockUser.tvm(dispenserTvmSourceId))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.0.serviceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Services in the sandbox are not allowed."));
                });
    }
}
