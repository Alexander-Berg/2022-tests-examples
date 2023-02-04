package ru.yandex.intranet.d.factory;

import java.util.List;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.datasource.impl.YdbRetry;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.metrics.YdbMetrics;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.BillingMeta;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.services.integration.providers.grpc.GrpcChannelFactoryService;

/**
 * GrpcChannelFactoryService tests.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 * @since 26-01-2021
 */
@IntegrationTest
public class GrpcChannelFactoryServiceTest {
    @Autowired
    GrpcChannelFactoryService grpcChannelFactoryService;
    @Autowired
    private ProvidersDao providersDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private YdbMetrics ydbMetrics;
    @Value("${ydb.transactionRetries}")
    private long transactionRetries;
    private static final long DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE = 1000L;

    @Test
    public void getChannelForProviderWithoutTLSTest() {
        ProviderModel provider = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                "https://test.yandex-team.ru", "in-process:test",
                999L, 998L, 9999L, false, false, false, false, true, "test-1",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, false, null, null),
                true, true, true, false, new BillingMeta(null), null, 1L, null, false, null, null, null, null);

        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProvidersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(provider))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        ManagedChannel block = grpcChannelFactoryService.get(provider.getId(), Tenants.DEFAULT_TENANT_ID)
                .block();
        Assertions.assertNotNull(block);
    }

    @Test
    public void getChannelForProviderWithTLSTest() {
        ProviderModel provider = new ProviderModel(UUID.randomUUID().toString(), Tenants.DEFAULT_TENANT_ID,
                0L, "Test-1", "Тест-1", "Test description-1", "Тестовое описание-1",
                "https://test.yandex-team.ru", "in-process:test",
                999L, 998L, 9999L, false, false, false, false, true, "test-1",
                new AccountsSettingsModel(true, true, true, true, true, true, true, true, true, true, true, false,
                        false, DEFAULT_ACCOUNTS_SYNC_PAGE_SIZE, false, null, null),
                true, true, true, true, new BillingMeta(null), null, 1L, null, false, null, null, null, null);

        ydbTableClient.usingSessionMonoRetryable(session -> providersDao.upsertProvidersRetryable(session
                .asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE), List.of(provider))
                .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();

        ManagedChannel block = grpcChannelFactoryService.get(provider.getId(), Tenants.DEFAULT_TENANT_ID)
                .block();
        Assertions.assertNotNull(block);
    }
}
