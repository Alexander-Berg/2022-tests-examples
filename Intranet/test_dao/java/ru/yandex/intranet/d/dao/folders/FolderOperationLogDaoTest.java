package ru.yandex.intranet.d.dao.folders;

import java.util.List;
import java.util.Optional;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;

import static ru.yandex.intranet.d.TestFolderOperations.TEST_FOLDER_OPERATION_LOG_1;
import static ru.yandex.intranet.d.TestFolderOperations.TEST_FOLDER_OPERATION_LOG_2;
import static ru.yandex.intranet.d.TestFolderOperations.TEST_FOLDER_OPERATION_LOG_NEW;
import static ru.yandex.intranet.d.TestFolderOperations.TEST_FOLDER_OPERATION_LOG_NEW_2;
import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * FolderOperationLogDaoTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 26.10.2020
 */
@IntegrationTest
class FolderOperationLogDaoTest {
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    void getByIdStartTx() {
        WithTxId<Optional<FolderOperationLogModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_FOLDER_OPERATION_LOG_1.getIdentity(),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(TEST_FOLDER_OPERATION_LOG_1, res.get().get());
    }

    @Test
    void getByIds() {
        List<FolderOperationLogModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(
                                TEST_FOLDER_OPERATION_LOG_1.getIdentity(),
                                TEST_FOLDER_OPERATION_LOG_2.getIdentity()
                        ),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(2, models.size());
        Assertions.assertTrue(models.contains(TEST_FOLDER_OPERATION_LOG_1));
        Assertions.assertTrue(models.contains(TEST_FOLDER_OPERATION_LOG_2));
    }

    @Test
    void upsertOneTx() {

        ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.upsertOneTxRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TEST_FOLDER_OPERATION_LOG_NEW
                )
        ).block();

        WithTxId<Optional<FolderOperationLogModel>> res = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.getByIdStartTx(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TEST_FOLDER_OPERATION_LOG_NEW.getIdentity(),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(res);
        Assertions.assertTrue(res.get().isPresent());
        Assertions.assertEquals(TEST_FOLDER_OPERATION_LOG_NEW, res.get().get());
    }

    @Test
    void upsertAll() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.upsertAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(TEST_FOLDER_OPERATION_LOG_NEW, TEST_FOLDER_OPERATION_LOG_NEW_2)
                )
        ).block();

        List<FolderOperationLogModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(
                                TEST_FOLDER_OPERATION_LOG_1.getIdentity(),
                                TEST_FOLDER_OPERATION_LOG_NEW.getIdentity(),
                                TEST_FOLDER_OPERATION_LOG_NEW_2.getIdentity()
                        ),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(3, models.size());
        Assertions.assertTrue(models.contains(TEST_FOLDER_OPERATION_LOG_1));
        Assertions.assertTrue(models.contains(TEST_FOLDER_OPERATION_LOG_NEW));
        Assertions.assertTrue(models.contains(TEST_FOLDER_OPERATION_LOG_NEW_2));
    }

    @Test
    void remove() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.removeRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        TEST_FOLDER_OPERATION_LOG_1.getIdentity(),
                        DEFAULT_TENANT_ID,
                        2
                )
        ).block();
        List<FolderOperationLogModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_FOLDER_OPERATION_LOG_1.getIdentity(), TEST_FOLDER_OPERATION_LOG_2.getIdentity()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(TEST_FOLDER_OPERATION_LOG_2));
    }

    @Test
    void removeAll() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.removeAllRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(TEST_FOLDER_OPERATION_LOG_1.getIdentity()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        List<FolderOperationLogModel> models = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.getByIds(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TEST_FOLDER_OPERATION_LOG_1.getIdentity(), TEST_FOLDER_OPERATION_LOG_2.getIdentity()),
                        DEFAULT_TENANT_ID
                )
        ).block();

        Assertions.assertNotNull(models);
        Assertions.assertEquals(1, models.size());
        Assertions.assertTrue(models.contains(TEST_FOLDER_OPERATION_LOG_2));
    }

}
