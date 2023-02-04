package ru.yandex.intranet.d.web.api.delivery;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.delivery.DeliverableDeltaDto;
import ru.yandex.intranet.d.web.model.delivery.DeliverableMetaRequestDto;
import ru.yandex.intranet.d.web.model.delivery.provide.DeliveryAndProvideDestinationDto;
import ru.yandex.intranet.d.web.model.delivery.provide.DeliveryAndProvideRequestDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResources.YP_SSD_VLA;
import static ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_UID;

/**
 * Delivery and provide API test (pre validation errors).
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
public class DeliveryAndProvidePreValidateApiTest {

    @Autowired
    private WebTestClient webClient;
    @Value("${hardwareOrderService.tvmSourceId}")
    private long dispenserTvmSourceId;

    @Test
    public void testNoPermissions() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("1120000000000010")
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(TestProviders.YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
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
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/api/v1/delivery/_provide")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.FORBIDDEN)
                .expectBody(ErrorCollectionDto.class);
    }

    @Test
    public void testNotValidDeliveryId() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId("ABC")
                .authorUid("1120000000000010")
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(TestProviders.YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
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
                .isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("deliveryId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("'ABC' is invalid UUID."));
                });
    }

    @Test
    public void testNotFoundAuthorUid() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(TestProviders.YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
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
                    Assertions.assertTrue(errors.contains("Field is required."));
                });
    }

    @Test
    public void testNotFoundDeliverables() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("1120000000000010")
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
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Field is required."));
                });
    }

    @Test
    public void testNotFoundDeliverableData() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("1120000000000010")
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder().build())
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
                    Set<String> errorsServiceId = errorCollection.getFieldErrors().get("deliverables.0.serviceId");
                    Assertions.assertNotNull(errorsServiceId);
                    Assertions.assertTrue(errorsServiceId.contains("Field is required."));
                    Set<String> errorsProviderId = errorCollection.getFieldErrors().get("deliverables.0.providerId");
                    Assertions.assertNotNull(errorsProviderId);
                    Assertions.assertTrue(errorsProviderId.contains("Field is required."));
                    Set<String> errorsFolderId = errorCollection.getFieldErrors().get("deliverables.0.folderId");
                    Assertions.assertNotNull(errorsFolderId);
                    Assertions.assertTrue(errorsFolderId.contains("Field is required."));
                    Set<String> errorsResourceId = errorCollection.getFieldErrors().get("deliverables.0.resourceId");
                    Assertions.assertNotNull(errorsResourceId);
                    Assertions.assertTrue(errorsResourceId.contains("Field is required."));
                    Set<String> errorsAccountId = errorCollection.getFieldErrors().get("deliverables.0.accountId");
                    Assertions.assertNotNull(errorsAccountId);
                    Assertions.assertTrue(errorsAccountId.contains("Field is required."));
                    Set<String> errorsDelta = errorCollection.getFieldErrors().get("deliverables.0.delta");
                    Assertions.assertNotNull(errorsDelta);
                    Assertions.assertTrue(errorsDelta.contains("Field is required."));
                    Set<String> errorsMeta = errorCollection.getFieldErrors().get("deliverables.0.meta");
                    Assertions.assertNotNull(errorsMeta);
                    Assertions.assertTrue(errorsMeta.contains("Field is required."));
                });
    }

    @Test
    public void testNotValidDeltaData() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("1120000000000010")
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(TestProviders.YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(0)
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
                    Set<String> errorsDeltaAmount = errorCollection.getFieldErrors().get("deliverables.0.delta.amount");
                    Assertions.assertNotNull(errorsDeltaAmount);
                    Assertions.assertTrue(errorsDeltaAmount.contains("Number must be positive."));
                    Set<String> errorsDeltaUnitKey = errorCollection.getFieldErrors()
                            .get("deliverables.0.delta.unitKey");
                    Assertions.assertNotNull(errorsDeltaUnitKey);
                    Assertions.assertTrue(errorsDeltaUnitKey.contains("Field is required."));
                });
    }

    @Test
    public void testNotFoundMetaData() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid("1120000000000010")
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(TestProviders.YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
                        .resourceId(TestResources.YP_HDD_SAS)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(100L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
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
                    Set<String> errorsMetaBigOrderId = errorCollection.getFieldErrors()
                            .get("deliverables.0.meta.bigOrderId");
                    Assertions.assertNotNull(errorsMetaBigOrderId);
                    Assertions.assertTrue(errorsMetaBigOrderId.contains("Field is required."));
                    Set<String> errorsMetaCampaignId = errorCollection.getFieldErrors()
                            .get("deliverables.0.meta.campaignId");
                    Assertions.assertNotNull(errorsMetaCampaignId);
                    Assertions.assertTrue(errorsMetaCampaignId.contains("Field is required."));
                    Set<String> errorsMetaQuotaRequestId = errorCollection.getFieldErrors()
                            .get("deliverables.0.meta.quotaRequestId");
                    Assertions.assertNotNull(errorsMetaQuotaRequestId);
                    Assertions.assertTrue(errorsMetaQuotaRequestId.contains("Field is required."));
                });
    }

    @Test
    public void testDeliveryDuplicateResourcesInOneAccount() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
                        .resourceId(YP_SSD_VLA)
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
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
                        .resourceId(YP_SSD_VLA)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(200L)
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
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.1");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Duplicate resource account pair: account id '" +
                            TEST_ACCOUNT_1_ID + "', and resource id '" + YP_SSD_VLA + "', and order id '42'."));
                });
    }

    @Test
    public void testDeliveryDifferntCampaign() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
                        .resourceId(YP_SSD_VLA)
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
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
                        .resourceId(YP_SSD_VLA)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(200L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(69L)
                                .campaignId(2L)
                                .bigOrderId(43L)
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
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.1.meta.campaignId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Different campaign id: '" + 2 + "', is not allowed."));
                });
    }

    @Test
    public void testDeliveryDifferentRequestId() {
        DeliveryAndProvideRequestDto body = new DeliveryAndProvideRequestDto.Builder()
                .deliveryId(UUID.randomUUID().toString())
                .authorUid(SERVICE_1_QUOTA_MANAGER_UID)
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
                        .resourceId(YP_SSD_VLA)
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
                .addDeliverable(new DeliveryAndProvideDestinationDto.Builder()
                        .serviceId(1L)
                        .providerId(YP_ID)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TEST_ACCOUNT_1_ID)
                        .resourceId(YP_SSD_VLA)
                        .delta(DeliverableDeltaDto.builder()
                                .amount(200L)
                                .unitKey("bytes")
                                .build())
                        .meta(DeliverableMetaRequestDto.builder()
                                .quotaRequestId(70L)
                                .campaignId(1L)
                                .bigOrderId(43L)
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
                    Set<String> errors = errorCollection.getFieldErrors().get("deliverables.1.meta.quotaRequestId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Different request id: '" + 70 + "', is not allowed."));
                });
    }
}
