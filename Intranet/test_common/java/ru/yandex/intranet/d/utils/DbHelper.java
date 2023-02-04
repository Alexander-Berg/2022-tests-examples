package ru.yandex.intranet.d.utils;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasOperationsDao;
import ru.yandex.intranet.d.dao.accounts.OperationsInProgressDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.dao.resources.ResourcesDao;
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.datasource.model.YdbTxSession;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.accounts.OperationInProgressModel;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel;

import static com.yandex.ydb.table.transaction.TransactionMode.SERIALIZABLE_READ_WRITE;

/**
 * DbHelper.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 09-09-2021
 */
@Component
public class DbHelper {
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private ProvidersDao providersDao;
    @Autowired
    private ResourceTypesDao resourceTypesDao;
    @Autowired
    private ResourcesDao resourcesDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    private AccountsQuotasOperationsDao accountsQuotasOperationsDao;
    @Autowired
    private OperationsInProgressDao operationsInProgressDao;

    public void writeDb(Function<YdbTxSession, Mono<Void>> body) {
        tableClient.usingSessionMonoRetryable(ts -> ts.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, body))
                .block();
    }

    public <T> T readDb(Function<YdbTxSession, Mono<T>> body) {
        return tableClient.usingSessionMonoRetryable(ts -> ts.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE, body))
                .block();
    }

    public void upsertProvider(ProviderModel provider) {
        writeDb(tx -> providersDao.upsertProviderRetryable(tx, provider));
    }

    public void upsertResourceType(ResourceTypeModel resourceType) {
        writeDb(tx -> resourceTypesDao.upsertResourceTypeRetryable(tx, resourceType));
    }

    public void upsertResource(ResourceModel resource) {
        writeDb(tx -> resourcesDao.upsertResourceRetryable(tx, resource));
    }

    public void upsertFolder(FolderModel folder) {
        writeDb(tx -> folderDao.upsertOneRetryable(tx, folder).then());
    }

    public void upsertQuota(QuotaModel quota) {
        writeDb(tx -> quotasDao.upsertOneRetryable(tx, quota).then());
    }

    public void upsertAccount(AccountModel account) {
        writeDb(tx -> accountsDao.upsertOneRetryable(tx, account).then());
    }

    public void upsertAccountsQuota(AccountsQuotasModel provision) {
        writeDb(tx -> accountsQuotasDao.upsertOneRetryable(tx, provision).then());
    }

    public void upsertAccountsQuotasOperation(AccountsQuotasOperationsModel operation) {
        writeDb(tx -> accountsQuotasOperationsDao.upsertOneRetryable(tx, operation).then());
    }

    public void upsertOperationsInProgress(OperationInProgressModel inProgress) {
        writeDb(tx -> operationsInProgressDao.upsertOneRetryable(tx, inProgress).then());
    }
}
