package ru.yandex.intranet.d.dao;

import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.datasource.model.TransactionLocksInvalidatedException;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.quotas.QuotaModel;

/**
 * Transaction locks test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class TransactionLocksTest {

    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private QuotasDao quotasDao;

    @Test
    public void testLockInvalidation() {
        QuotaModel newQuota = ydbTableClient.usingSessionMonoRetryable(session -> session
                .usingTxMono(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.upsertOneRetryable(txSession, QuotaModel.builder()
                                .tenantId(Tenants.DEFAULT_TENANT_ID)
                                .providerId(UUID.randomUUID().toString())
                                .resourceId(UUID.randomUUID().toString())
                                .folderId(UUID.randomUUID().toString())
                                .quota(0L)
                                .balance(0L)
                                .frozenQuota(0L)
                                .build()))).block();
        Boolean success = ydbTableClient.usingSessionMonoRetryable(outerSession -> outerSession
                .usingTxMono(TransactionMode.SERIALIZABLE_READ_WRITE, outerTxSession -> quotasDao
                        .getById(outerTxSession, newQuota.toKey(), Tenants.DEFAULT_TENANT_ID)
                .flatMap(outerQuotaRead -> ydbTableClient.usingSessionMonoRetryable(innerSession -> innerSession
                        .usingTxMono(TransactionMode.SERIALIZABLE_READ_WRITE, innerTxSession -> quotasDao
                                .getById(innerTxSession, newQuota.toKey(), Tenants.DEFAULT_TENANT_ID)
                        .flatMap(innerQuotaRead -> quotasDao.upsertOneRetryable(innerTxSession,
                                QuotaModel.builder(innerQuotaRead.get()).balance(1L).build()))))
                        .flatMap(innerResult -> quotasDao.upsertOneRetryable(outerTxSession,
                                QuotaModel.builder(outerQuotaRead.get()).balance(2L).build())))))
                .map(m -> false).onErrorReturn(TransactionLocksInvalidatedException::isTransactionLocksInvalidated,
                        true).block();
        Assertions.assertNotNull(success);
        Assertions.assertTrue(success);
    }

    @Test
    public void testLockInvalidationInlineInnerTx() {
        QuotaModel newQuota = ydbTableClient.usingSessionMonoRetryable(session -> session
                .usingTxMono(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.upsertOneRetryable(txSession, QuotaModel.builder()
                                .tenantId(Tenants.DEFAULT_TENANT_ID)
                                .providerId(UUID.randomUUID().toString())
                                .resourceId(UUID.randomUUID().toString())
                                .folderId(UUID.randomUUID().toString())
                                .quota(0L)
                                .balance(0L)
                                .frozenQuota(0L)
                                .build()))).block();
        Boolean success = ydbTableClient.usingSessionMonoRetryable(outerSession -> outerSession
                .usingTxMono(TransactionMode.SERIALIZABLE_READ_WRITE, outerTxSession -> quotasDao
                        .getById(outerTxSession, newQuota.toKey(), Tenants.DEFAULT_TENANT_ID)
                        .flatMap(outerQuotaRead -> ydbTableClient.usingSessionMonoRetryable(innerSession ->
                                quotasDao.upsertOneRetryable(innerSession
                                                .asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                                        QuotaModel.builder(newQuota).balance(1L).build()))
                                .flatMap(innerResult -> quotasDao.upsertOneRetryable(outerTxSession,
                                        QuotaModel.builder(outerQuotaRead.get()).balance(2L).build())))))
                .map(m -> false).onErrorReturn(TransactionLocksInvalidatedException::isTransactionLocksInvalidated,
                        true).block();
        Assertions.assertNotNull(success);
        Assertions.assertTrue(success);
    }

    @Test
    public void testLockInvalidationRetry() {
        QuotaModel newQuota = ydbTableClient.usingSessionMonoRetryable(session -> session
                .usingTxMono(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.upsertOneRetryable(txSession, QuotaModel.builder()
                                .tenantId(Tenants.DEFAULT_TENANT_ID)
                                .providerId(UUID.randomUUID().toString())
                                .resourceId(UUID.randomUUID().toString())
                                .folderId(UUID.randomUUID().toString())
                                .quota(0L)
                                .balance(0L)
                                .frozenQuota(0L)
                                .build()))).block();
        Boolean success = ydbTableClient.usingSessionMonoRetryable(outerSession -> outerSession
                .usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, outerTxSession -> quotasDao
                        .getById(outerTxSession, newQuota.toKey(), Tenants.DEFAULT_TENANT_ID)
                        .flatMap(outerQuotaRead -> ydbTableClient.usingSessionMonoRetryable(innerSession ->
                                innerSession.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                        innerTxSession -> quotasDao.getById(innerTxSession, newQuota.toKey(),
                                                Tenants.DEFAULT_TENANT_ID)
                                        .flatMap(innerQuotaRead -> quotasDao.upsertOneRetryable(innerTxSession,
                                                QuotaModel.builder(innerQuotaRead.get()).balance(1L).build()))))
                                .flatMap(innerResult -> quotasDao.upsertOneRetryable(outerTxSession,
                                        QuotaModel.builder(outerQuotaRead.get()).balance(2L).build())))))
                .map(m -> false).onErrorReturn(TransactionLocksInvalidatedException::isTransactionLocksInvalidated,
                        true).block();
        Assertions.assertNotNull(success);
        Assertions.assertTrue(success);
    }

}
