package ru.yandex.solomon.alert.dao.ydb;

import java.util.concurrent.CompletionException;

import com.yandex.ydb.table.transaction.TxControl;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class YdbExceptionHandlerTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();
    private static YdbHelper ydb;

    @BeforeClass
    public static void beforeClass() {
        ydb = new YdbHelper(kikimr, "kikimr");
    }

    @Test
    public void pathDoesNotExists() {
        try {
            ydb.getTableClient().createSession()
                .thenApply(result -> result.expect("success create session"))
                .thenCompose(session -> session.executeDataQuery("SELECT * from `/Root/NotExistsTable` LIMIT 1;", TxControl.serializableRw()))
                .thenApply(result -> result.expect("should failed"))
                .join();
            fail("query should fail because table not exists");
        } catch (Throwable e) {
            assertTrue(YdbExceptionHandler.isPathDoesNotExist(e));
        }
    }

    @Test
    public void noPathDoesNotExist() {
        assertFalse(YdbExceptionHandler.isPathDoesNotExist(new RuntimeException("hi")));
        assertFalse(YdbExceptionHandler.isPathDoesNotExist(new CompletionException(new RuntimeException("hi"))));
        assertFalse(YdbExceptionHandler.isPathDoesNotExist(new NullPointerException()));
    }
}
