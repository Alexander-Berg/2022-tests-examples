package ru.yandex.intranet.d.loaders.providers;

import java.util.List;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.providers.ProviderModel;

/**
 * Providers loader test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ProvidersLoaderTest {

    @Autowired
    private ProvidersLoader providersLoader;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void testGetProviderBySourceTvmId() {
        List<ProviderModel> ypProviderFirst = ydbTableClient.usingSessionMonoRetryable(session ->
                providersLoader.getProviderBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)).block();
        List<ProviderModel> ypProviderSecond = ydbTableClient.usingSessionMonoRetryable(session ->
                providersLoader.getProviderBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(ypProviderFirst);
        Assertions.assertFalse(ypProviderFirst.isEmpty());
        Assertions.assertNotNull(ypProviderSecond);
        Assertions.assertFalse(ypProviderSecond.isEmpty());
    }

    @Test
    public void testGetProviderBySourceTvmIdMissing() {
        List<ProviderModel> ypProviderFirst = ydbTableClient.usingSessionMonoRetryable(session ->
                providersLoader.getProviderBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        -1L, Tenants.DEFAULT_TENANT_ID)).block();
        List<ProviderModel> ypProviderSecond = ydbTableClient.usingSessionMonoRetryable(session ->
                providersLoader.getProviderBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        -1L, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(ypProviderFirst);
        Assertions.assertTrue(ypProviderFirst.isEmpty());
        Assertions.assertNotNull(ypProviderSecond);
        Assertions.assertTrue(ypProviderSecond.isEmpty());
    }

    @Test
    public void testGetProviderBySourceTvmIdImmediate() {
        List<ProviderModel> ypProviderFirst = providersLoader
                .getProviderBySourceTvmIdImmediate(TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID).block();
        List<ProviderModel> ypProviderSecond = providersLoader
                .getProviderBySourceTvmIdImmediate(TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID).block();
        Assertions.assertNotNull(ypProviderFirst);
        Assertions.assertFalse(ypProviderFirst.isEmpty());
        Assertions.assertNotNull(ypProviderSecond);
        Assertions.assertFalse(ypProviderSecond.isEmpty());
    }

    @Test
    public void testGetProviderBySourceTvmIdImmediateMissing() {
        List<ProviderModel> ypProviderFirst = providersLoader
                .getProviderBySourceTvmIdImmediate(-1L, Tenants.DEFAULT_TENANT_ID).block();
        List<ProviderModel> ypProviderSecond = providersLoader
                .getProviderBySourceTvmIdImmediate(-1L, Tenants.DEFAULT_TENANT_ID).block();
        Assertions.assertNotNull(ypProviderFirst);
        Assertions.assertTrue(ypProviderFirst.isEmpty());
        Assertions.assertNotNull(ypProviderSecond);
        Assertions.assertTrue(ypProviderSecond.isEmpty());
    }

    @Test
    public void testRefresh() {
        List<ProviderModel> ypProviderBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                providersLoader.getProviderBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)).block();
        List<ProviderModel> noProviderBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                providersLoader.getProviderBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        -1L, Tenants.DEFAULT_TENANT_ID)).block();
        providersLoader.refreshCache();
        List<ProviderModel> ypProviderAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                providersLoader.getProviderBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestProviders.YP_SOURCE_TVM_ID, Tenants.DEFAULT_TENANT_ID)).block();
        List<ProviderModel> noProviderAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                providersLoader.getProviderBySourceTvmId(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        -1L, Tenants.DEFAULT_TENANT_ID)).block();
        Assertions.assertNotNull(ypProviderBefore);
        Assertions.assertFalse(ypProviderBefore.isEmpty());
        Assertions.assertNotNull(noProviderBefore);
        Assertions.assertTrue(noProviderBefore.isEmpty());
        Assertions.assertNotNull(ypProviderAfter);
        Assertions.assertFalse(ypProviderAfter.isEmpty());
        Assertions.assertNotNull(noProviderAfter);
        Assertions.assertTrue(noProviderAfter.isEmpty());
    }

}
