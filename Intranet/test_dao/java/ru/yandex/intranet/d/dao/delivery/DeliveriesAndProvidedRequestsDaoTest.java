package ru.yandex.intranet.d.dao.delivery;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestAccounts;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.delivery.DeliverableDeltaModel;
import ru.yandex.intranet.d.model.delivery.DeliverableMetaRequestModel;
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideDestinationModel;
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideModel;
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideOperationListModel;
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideOperationModel;
import ru.yandex.intranet.d.model.delivery.provide.DeliveryAndProvideRequestModel;

import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * Delivery DAO test.
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
class DeliveriesAndProvidedRequestsDaoTest {
    private static final String TEST_DELIVERY_1_ID = "1421ae7c-9b76-44bc-87c7-e18d998778b3";
    private static final String TEST_DELIVERY_2_ID = "5f85f29a-f8d8-472d-ab09-6830cf1fee50";

    @Autowired
    private DeliveriesAndProvidedRequestsDao deliveriesDao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    void getByIdStartTx() {
        WithTxId<Optional<DeliveryAndProvideModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                deliveriesDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_DELIVERY_1_ID,
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(TEST_DELIVERY_1_ID, res.get().get().getDeliveryId());
    }

    @Test
    void getByIds() {
        List<DeliveryAndProvideModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                deliveriesDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_DELIVERY_1_ID, TEST_DELIVERY_2_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertTrue(models.stream().anyMatch(delivery ->
                delivery.getDeliveryId().equals(TEST_DELIVERY_1_ID)));
        Assertions.assertTrue(models.stream().anyMatch(delivery ->
                delivery.getDeliveryId().equals(TEST_DELIVERY_2_ID)));
    }

    @Test
    void upsertOneTx() {
        String newDeliveryId = "0dbd459b-b7ea-4f0c-9450-5ecba65becec";
        DeliveryAndProvideModel newDelivery = createFinishedDeliveryModel(newDeliveryId);

        ydbTableClient.usingSessionMonoRetryable(session ->
                deliveriesDao.upsertOneTxRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        newDelivery
                )
        ).block();

        WithTxId<Optional<DeliveryAndProvideModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                deliveriesDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        newDeliveryId,
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(newDelivery, res.get().get());
    }

    @Test
    void upsertAll() {
        String newDelivery1Id = "0dbd459b-b7ea-4f0c-9450-5ecba65becec";
        DeliveryAndProvideModel newDelivery1 = createFinishedDeliveryModel(newDelivery1Id);

        String newDelivery2Id = "8a3a68f2-031a-4512-a7c5-a4442c11b4bc";
        DeliveryAndProvideModel newDelivery2 = createFinishedDeliveryModel(newDelivery2Id);

        ydbTableClient.usingSessionMonoRetryable(session ->
                deliveriesDao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(newDelivery1, newDelivery2)
                )
        ).block();

        List<DeliveryAndProvideModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                deliveriesDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(newDelivery1Id, newDelivery2Id),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertEquals(Set.of(newDelivery1, newDelivery2), new HashSet<>(models));
    }

    private DeliveryAndProvideModel createFinishedDeliveryModel(String deliveryId) {
        return new DeliveryAndProvideModel.Builder()
                .deliveryId(deliveryId)
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .request(new DeliveryAndProvideRequestModel.Builder()
                        .deliveryId(deliveryId)
                        .authorUid("1120000000000010")
                        .addDeliverable(new DeliveryAndProvideDestinationModel.Builder()
                                .serviceId(1L)
                                .providerId(TestProviders.YP_ID)
                                .folderId(TestFolders.TEST_FOLDER_1_ID)
                                .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                                .resourceId(TestResources.YP_HDD_SAS)
                                .delta(DeliverableDeltaModel.builder()
                                        .amount(100L)
                                        .unitKey("bytes")
                                        .build())
                                .meta(DeliverableMetaRequestModel.builder()
                                        .quotaRequestId(69L)
                                        .campaignId(1L)
                                        .bigOrderId(42L)
                                        .build())
                                .build())
                        .build())
                .addOperation(new DeliveryAndProvideOperationListModel.Builder()
                        .serviceId(1L)
                        .folderId(TestFolders.TEST_FOLDER_1_ID)
                        .accountId(TestAccounts.TEST_ACCOUNT_1_ID)
                        .addOperation(new DeliveryAndProvideOperationModel.Builder()
                                .operationId(UUID.randomUUID().toString())
                                .version(0)
                                .build())
                        .build())
                .build();
    }
}
