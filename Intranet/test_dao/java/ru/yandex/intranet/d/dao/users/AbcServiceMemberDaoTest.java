package ru.yandex.intranet.d.dao.users;

import java.util.List;
import java.util.Set;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.datasource.impl.YdbRetry;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.metrics.YdbMetrics;
import ru.yandex.intranet.d.model.users.AbcServiceMemberModel;

/**
 * Users DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class AbcServiceMemberDaoTest {

    @Autowired
    private AbcServiceMemberDao abcServiceMemberDao;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private YdbMetrics ydbMetrics;
    @Value("${ydb.transactionRetries}")
    private long transactionRetries;

    @Test
    public void testGetByUsersAndRoles() {
        List<AbcServiceMemberModel> members = ydbTableClient.usingSessionMonoRetryable(session ->
                abcServiceMemberDao.getByUsersAndRoles(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Set.of(9990L, 9991L, 9992L), Set.of(8880L, 8881L, 8882L), 1)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(members);
        Assertions.assertEquals(5, members.size());
    }

    @Test
    public void testGetByServicesAndRoles() {
        List<AbcServiceMemberModel> members = ydbTableClient.usingSessionMonoRetryable(session ->
                abcServiceMemberDao.getByServicesAndRoles(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Set.of(5550L, 5551L, 5552L), Set.of(8880L, 8881L, 8882L), 1)
                        .retryWhen(YdbRetry.retryIdempotentTransaction(transactionRetries, ydbMetrics))).block();
        Assertions.assertNotNull(members);
        Assertions.assertEquals(5, members.size());
    }

}
