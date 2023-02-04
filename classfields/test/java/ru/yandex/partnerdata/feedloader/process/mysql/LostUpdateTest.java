package ru.yandex.partnerdata.feedloader.process.mysql;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.common.util.db.NamedDataSource;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Isolation level test
 *
 * @author sunlight
 */
@Ignore

public class LostUpdateTest {

    private static Logger log = LogManager.getLogger(LostUpdateTest.class);;

    private final String dbUrl = "jdbc:mysql://unittest-02-sas.dev.vertis.yandex.net";
    private final String username = "feedloader";
    private final String password = "feedloader";

    private final int MAX_ACTIVE_CONNECTIONS = 10;
    private final int MAX_WAIT_IN_MILLIS = 1000;
    private final int REMOVE_ABANDONED_TIMEOUT = 300;

    private final int ISOLATION_LEVEL = Connection.TRANSACTION_READ_UNCOMMITTED;
    private final int TRANSACTION_TIMEOUT = 1000;

    private final NamedDataSource ds = createDataSource();
    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
    private final DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
    private final TransactionTemplate tt = createTransactionTemplate(tm);

    private final int CONCURRENCY_FACTOR = 10;
    private final int TOTAL_OPERATIONS_AMOUNT = 100;
//    private final int TOTAL_OPERATIONS_AMOUNT = 1000;
    private final static String tag = nextSessionId();
    private final static String dbName = "feedloader_test_" + tag;
    private final ExecutorService es = Executors.newFixedThreadPool(CONCURRENCY_FACTOR);

    private NamedDataSource createDataSource() {
        final NativeJdbcExtractor extractor = new CommonsDbcpNativeJdbcExtractor();

        final NamedDataSource ds = new NamedDataSource();
        ds.setNativeJdbcExtractor(extractor);
        ds.setModuleName(dbName);
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUrl(dbUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setValidationQuery("select 1 from dual");
        ds.setMaxActive(MAX_ACTIVE_CONNECTIONS);
        ds.setMaxWait(MAX_WAIT_IN_MILLIS);
        ds.setRemoveAbandoned(true);
        ds.setRemoveAbandonedTimeout(REMOVE_ABANDONED_TIMEOUT);

        return ds;
    }

    private TransactionTemplate createTransactionTemplate(final DataSourceTransactionManager tm) {
        final TransactionTemplate tt = new TransactionTemplate(tm);
        tt.setIsolationLevel(ISOLATION_LEVEL);
        tt.setReadOnly(false);
        tt.setTimeout(TRANSACTION_TIMEOUT);
        return tt;
    }

    @Before
    public void initDb() {
        log.debug("creating db with name " + dbName);

        final boolean result = tt.execute(
                new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {

                        try {
                            jdbcTemplate.execute("CREATE DATABASE " + dbName);
                            jdbcTemplate.execute("USE " + dbName);
                            jdbcTemplate.execute(
                                    "CREATE TABLE counter " +
                                            "(`value` bigint(20) NOT NULL DEFAULT 0" +
                                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

                            return true;
                        } catch (Exception e) {
                            log.warn("error in db", e);
                            status.setRollbackOnly();
                        }
                        return false;
                    }
                }
        );
        if (result) {
            log.debug("db with name " + dbName + " created successfully");
        } else {
            log.debug("db with name " + dbName + " creation failed");
        }
    }

    @After
    public void deleteDb() {
        //jdbcTemplate.execute("DROP DATABASE " + dbName);
        log.debug("db with name " + dbName + " dropped");
    }


    @Test
    public void testSmth() {
        final AtomicLong atomicCounter = new AtomicLong();
        jdbcTemplate.execute("INSERT INTO counter (`value`) VALUES (0)");

        final List<Callable<Boolean>> allCallables = new ArrayList<Callable<Boolean>>(TOTAL_OPERATIONS_AMOUNT);
        for (int i = 0; i < TOTAL_OPERATIONS_AMOUNT; i++) {
            allCallables.add(
                    new Callable<Boolean>() {
                        @Override
                        public Boolean call() {
                            final boolean result = tt.execute(incremntationTransaction());
                            if (result) {
                               final long currentValue = atomicCounter.incrementAndGet();
                               log.debug("atomic current value: " + currentValue);
                            }
                            return result;
                        }
                    });
        }

        try {
            es.invokeAll(allCallables);
        } catch (InterruptedException e) {
            log.error("InterruptedException happend", e);
        }

        log.debug("here is result...");

        log.debug("ATOMIC COUNTER: " + atomicCounter.get());
        int counterFinalVal = jdbcTemplate.queryForInt("SELECT `value` FROM counter LIMIT 1");
        log.debug("COUNTER FINAL VALUE: " + counterFinalVal);
        assert(counterFinalVal == atomicCounter.get());
    }


    private TransactionCallback<Boolean> incremntationTransaction() {
        return new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                try {
                    jdbcTemplate.execute("USE " + dbName);
                    int counter = jdbcTemplate.queryForInt("SELECT `value` FROM counter LIMIT 1 FOR UPDATE");
                    log.debug("current counter value: " + counter);
                    final boolean res = jdbcTemplate.update("update counter set `value` = ?", counter + 1) > 0;
                    return res;
                } catch (Exception e) {
                    log.error("transaction error", e);
                    status.setRollbackOnly();
                }
                return false;
            }
        };
    }

    private static String nextSessionId() {
        final SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }
}
