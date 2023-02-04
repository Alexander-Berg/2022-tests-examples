package ru.yandex.intranet.d.dao.accounts;


import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel;
import ru.yandex.intranet.d.model.providers.ProviderId;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_1;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_3;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_4;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_5;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_6;
import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * AccountsSpacesDaoTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 10.12.2020
 */
@IntegrationTest
class AccountsSpacesDaoTest {
    @Autowired
    private AccountsSpacesDao dao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    void getByIdStartTx() {
        WithTxId<Optional<AccountSpaceModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_ACCOUNT_SPACE_1.getId(),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1, res.get().get());
    }

    @Test
    void getByIds() {
        List<AccountSpaceModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNT_SPACE_1.getId(), TEST_ACCOUNT_SPACE_2.getId()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_SPACE_1));
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_SPACE_2));
    }

    @Test
    void upsertOneTx() {
        String newAccountSpaceId = "c39c86ce-6e46-4d22-959c-47a96674ab3d";
        AccountSpaceModel newAccountSpace = AccountSpaceModel.newBuilder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(newAccountSpaceId)
                .setDeleted(false)
                .setNameEn("VLA default")
                .setNameRu("VLA default")
                .setDescriptionEn("VLA default")
                .setDescriptionRu("VLA default")
                .setProviderId(TestProviders.YP_ID)
                .setOuterKeyInProvider(null)
                .setVersion(0)
                .setSegments(Set.of(
                        ResourceSegmentSettingsModel.builder()
                                .segmentationId("7fbd778f-d803-44c8-831a-c1de5c05885c")
                                .segmentId("8f6a2b58-b10c-4742-bee6-b3587793b5e8")
                                .build(),
                        ResourceSegmentSettingsModel.builder()
                                .segmentationId("4654c7c8-cb87-4a73-8af4-0b8d4a92f16a")
                                .segmentId("e9552be0-7b24-4c70-a1e4-dd842299a802")
                                .build()
                ))
                .build();

        ydbTableClient.usingSessionMonoRetryable(session ->
                dao.upsertOneTxRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        newAccountSpace
                )
        ).block();

        WithTxId<Optional<AccountSpaceModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        newAccountSpaceId,
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(newAccountSpace, res.get().get());
    }

    @Test
    void remove() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                dao.removeRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TEST_ACCOUNT_SPACE_1.getId(),
                        DEFAULT_TENANT_ID,
                        2
                )
        ).block();
        List<AccountSpaceModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_ACCOUNT_SPACE_1.getId(), TEST_ACCOUNT_SPACE_2.getId()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(TEST_ACCOUNT_SPACE_2));
    }

    @Test
    void getBySegments() {
        WithTxId<Optional<AccountSpaceModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getBySegments(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        DEFAULT_TENANT_ID,
                        TestProviders.YP_ID,
                        TEST_ACCOUNT_SPACE_1.getSegments()
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_1, res.get().get());
    }

    @Test
    void getAllByProvider() {
        List<AccountSpaceModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getAllByProvider(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        DEFAULT_TENANT_ID,
                        TestProviders.YP_ID
                )
        ).block().get();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(6, models.size());
        Assertions.assertTrue(models.containsAll(List.of(
                TEST_ACCOUNT_SPACE_1,
                TEST_ACCOUNT_SPACE_2,
                TEST_ACCOUNT_SPACE_3,
                TEST_ACCOUNT_SPACE_4,
                TEST_ACCOUNT_SPACE_5,
                TEST_ACCOUNT_SPACE_6
        )));
    }

    @Test
    void getAllByProviderIds() {
        List<AccountSpaceModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                dao.getAllByProviderIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(new WithTenant<>(DEFAULT_TENANT_ID, new ProviderId(TestProviders.YP_ID)))
                )
        ).block().get();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(6, models.size());
        Assertions.assertTrue(models.containsAll(List.of(
                TEST_ACCOUNT_SPACE_1,
                TEST_ACCOUNT_SPACE_2,
                TEST_ACCOUNT_SPACE_3,
                TEST_ACCOUNT_SPACE_4,
                TEST_ACCOUNT_SPACE_5,
                TEST_ACCOUNT_SPACE_6
        )));
    }
}
