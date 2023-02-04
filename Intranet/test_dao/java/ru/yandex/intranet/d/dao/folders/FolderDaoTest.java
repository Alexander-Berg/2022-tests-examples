package ru.yandex.intranet.d.dao.folders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;

import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2_ID;
import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * FolderDaoTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 */
@IntegrationTest
class FolderDaoTest {
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void testGetById() {
        Optional<FolderModel> folderModel = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getById(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_FOLDER_1_ID, DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(folderModel);
        Assertions.assertTrue(folderModel.isPresent());
        Assertions.assertEquals(TEST_FOLDER_1, folderModel.get());
    }

    @Test
    public void testGetByIds() {
        List<FolderModel> folderModels = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_FOLDER_1_ID, TEST_FOLDER_2_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(folderModels);
        Assertions.assertTrue(folderModels.contains(TEST_FOLDER_1));
        Assertions.assertTrue(folderModels.contains(TEST_FOLDER_2));
    }

    @Test
    void testUpsertFolder() {
        FolderModel folderToWrite = new FolderModel(
                "dbb24abf-b300-4980-9fcd-798bfd63cab2",
                DEFAULT_TENANT_ID,
                1,
                0,
                "Проверочная папка 2",
                "Папка для проверки Upsert",
                false,
                FolderType.COMMON_DEFAULT_FOR_SERVICE,
                Set.of("prod", "white"),
                0L
        );
        ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.upsertOneRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        folderToWrite
                )
        ).block();

        Optional<FolderModel> folderToRead = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getById(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        folderToWrite.getId(), DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(folderToRead);
        Assertions.assertTrue(folderToRead.isPresent());
        Assertions.assertEquals(folderToWrite, folderToRead.get());
    }

    @Test
    public void testRemove() {
        Optional<FolderModel> folderModel = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getById(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_FOLDER_1_ID, DEFAULT_TENANT_ID
                )
        ).block();
        Assertions.assertNotNull(folderModel);
        Assertions.assertTrue(folderModel.isPresent());

        ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.removeRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TEST_FOLDER_1_ID, DEFAULT_TENANT_ID, 1
                )
        ).block();

        folderModel = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getById(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_FOLDER_1_ID, DEFAULT_TENANT_ID
                )
        ).block();
        Assertions.assertNotNull(folderModel);
        Assertions.assertFalse(folderModel.isPresent());
    }

    @Test
    public void testRemoveAll() {
        List<FolderModel> folderModels = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_FOLDER_1_ID, TEST_FOLDER_2_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();
        Assertions.assertNotNull(folderModels);
        Assertions.assertEquals(2, folderModels.size());

        ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.removeAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(TEST_FOLDER_1_ID, TEST_FOLDER_2_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();

        folderModels = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_FOLDER_1_ID, TEST_FOLDER_2_ID),
                        DEFAULT_TENANT_ID
                )
        ).block();
        Assertions.assertNotNull(folderModels);
        Assertions.assertEquals(0, folderModels.size());
    }

    @Test
    public void listFoldersByServiceIdTest() {
        WithTxId<List<FolderModel>> folders = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.listFoldersByServiceIdWithoutDefaultTx(
                        session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        DEFAULT_TENANT_ID, 1, false,
                        100, null
                )
        ).block();

        Assertions.assertNotNull(folders);
        List<String> folderIds = folders.get().stream().map(FolderModel::getId).collect(Collectors.toList());
        Assertions.assertTrue(folderIds.contains(TEST_FOLDER_1_ID));
    }

    @Test
    public void getAllByServiceIds() {
        int count = 1100;
        List<FolderModel> foldersToUpsert = new ArrayList<>(count);
        long serviceId1 = 1000;
        long serviceId2 = 1001;
        Set<WithTenant<Long>> serviceIds = Set.of(
                new WithTenant<>(DEFAULT_TENANT_ID, serviceId1),
                new WithTenant<>(DEFAULT_TENANT_ID, serviceId2));
        for (int i = 0; i < count / 2; i++) {
            foldersToUpsert.add(new FolderModel(
                    UUID.randomUUID().toString(),
                    DEFAULT_TENANT_ID,
                    serviceId1,
                    0,
                    "Проверочная папка в первом сервисе",
                    "Папка для проверки",
                    false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE,
                    Set.of("prod", "white"),
                    0L
            ));
            foldersToUpsert.add(new FolderModel(
                    UUID.randomUUID().toString(),
                    DEFAULT_TENANT_ID,
                    serviceId2,
                    0,
                    "Проверочная папка во втором сервисе",
                    "Папка для проверки",
                    false,
                    FolderType.COMMON_DEFAULT_FOR_SERVICE,
                    Set.of("prod", "white"),
                    0L
            ));
        }

        ydbTableClient.usingSessionMonoRetryable(session -> folderDao.upsertAllRetryable(
                session.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE), foldersToUpsert)).block();

        WithTxId<List<FolderModel>> folders = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getAllFoldersByServiceIds(
                        session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY), serviceIds)).block();
        Assertions.assertNotNull(folders);
        Assertions.assertNotNull(folders.get());
        Assertions.assertEquals(count, folders.get().size());
        Assertions.assertTrue(folders.get().stream().allMatch(folder ->
                serviceIds.contains(new WithTenant<>(DEFAULT_TENANT_ID, folder.getServiceId()))));
    }
}
