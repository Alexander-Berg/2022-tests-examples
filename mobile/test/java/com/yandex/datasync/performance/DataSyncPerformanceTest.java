package com.yandex.datasync.performance;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.DatabasesRepository;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.database.sql.SQLiteDatabaseWrapper;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.local.GetSnapshotOperation;
import com.yandex.datasync.internal.operation.network.MockRawDataObserver;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static com.yandex.datasync.asserters.RecordsDtoAsserter.assertRecords;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class DataSyncPerformanceTest {

    private AtomicLong start;

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "user_schedule";

    private static final String MOCK_USER_ID = "mock_user_id";

    private DatabaseManager databaseManager;

    private final MockRawDataObserver observable = new MockRawDataObserver();

    @Before
    public void setUp() throws IOException {
        databaseManager = new DatabaseManager(RuntimeEnvironment.application);
        databaseManager.init(MOCK_USER_ID);
        databaseManager.openDatabaseWrapped(MOCK_CONTEXT, MOCK_DATABASE_ID);
        fillDatabaseInfo();

        start = new AtomicLong(System.currentTimeMillis());
    }

    @Test
    public void testLocalGetOperationPerformance() throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile("database_performance.json");

        System.out.println(
                "Performance test (read from json): " + getTimeFromPreviousPointMs() + " ms");

        final SnapshotResponse snapshot =
                new Moshi.Builder().build().adapter(SnapshotResponse.class)
                        .fromJson(jsonString);

        System.out.println(
                "Performance test (parse json): " + getTimeFromPreviousPointMs() + " ms");

        final SnapshotRepository repository = new SnapshotRepository(databaseManager,
                                                                     MOCK_CONTEXT,
                                                                     MOCK_DATABASE_ID);
        repository.save(snapshot);

        System.out.println("Performance test (save): " + getTimeFromPreviousPointMs() + " ms");

        final GetSnapshotOperation operation = new GetSnapshotOperation(MOCK_CONTEXT,
                                                                        MOCK_DATABASE_ID,
                                                                        databaseManager,
                                                                        observable);
        for (int i = 0; i < 1000; i++) {
            operation.run();
            observable.getSnapshot().getRecords();
        }

        System.out.println("Warmup (get) done for: " + getTimeFromPreviousPointMs() + " ms");

        for (int i = 0; i < 10_000; i++) {
            operation.run();
            observable.getSnapshot().getRecords();
        }

        System.out.println(
                "Performance test (get): " + getTimeFromPreviousPointMs() / 10_000d + " ms");
        assertRecords(observable.getSnapshot().getRecords(), snapshot.getRecords());
    }

    private long getTimeFromPreviousPointMs() {
        final long timestamp = System.currentTimeMillis();
        return timestamp - start.getAndSet(timestamp);
    }

    private void fillDatabaseInfo() throws IOException {
        final SQLiteDatabaseWrapper databaseWrapper =
                databaseManager.openDatabaseWrapped(MOCK_CONTEXT);
        final DatabasesRepository changesRepository = new DatabasesRepository(databaseWrapper);
        final String databaseInfo = ResourcesUtil.getTextFromFile("get_database_info.json");
        final DatabaseDto databaseDto = new Moshi.Builder().build().adapter(DatabaseDto.class)
                .fromJson(databaseInfo);
        changesRepository.save(databaseDto);
    }
}
