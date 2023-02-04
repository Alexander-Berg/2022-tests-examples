package ru.yandex.intranet.d.tms.jobs;

import java.util.List;
import java.util.Optional;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.services.ServicesDao;
import ru.yandex.intranet.d.datasource.model.WithTxId;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.folders.FolderModel;

/**
 * Test for cron job to add missing default folders for services.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class AddDefaultFoldersTest {

    @Autowired
    private AddDefaultFolders addDefaultFolders;
    @Autowired
    private ServicesDao servicesDao;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private YdbTableClient tableClient;

    @Test
    public void testAdd() {
        addDefaultFolders.execute();
        List<Long> serviceIds = tableClient
                .usingSessionMonoRetryable(session -> servicesDao.getAllServiceIds(session).collectList()).block();
        Assertions.assertNotNull(serviceIds);
        serviceIds.forEach(id -> {
            Optional<FolderModel> defaultFolder = tableClient.usingSessionMonoRetryable(s -> folderDao
                    .getDefaultFolderTx(s.asTxCommit(TransactionMode.SERIALIZABLE_READ_WRITE),
                            Tenants.DEFAULT_TENANT_ID, id)).map(WithTxId::get).block();
            Assertions.assertNotNull(defaultFolder);
            Assertions.assertTrue(defaultFolder.isPresent());
        });
    }

}
