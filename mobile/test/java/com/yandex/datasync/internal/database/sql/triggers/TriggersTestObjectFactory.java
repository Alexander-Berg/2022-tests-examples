package com.yandex.datasync.internal.database.sql.triggers;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.database.repository.SnapshotRepository;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.util.ResourcesUtil;

import java.io.IOException;

public class TriggersTestObjectFactory {

    public static void fillDatabase(DatabaseManager databaseManager,
                                    YDSContext context,
                                    String databaseId, String fileName)
            throws IOException {
        final String jsonString = ResourcesUtil.getTextFromFile(fileName);
        final SnapshotResponse snapshot =
                new Moshi.Builder().build().adapter(SnapshotResponse.class)
                        .fromJson(jsonString);
        final SnapshotRepository snapshotRepository = new SnapshotRepository(databaseManager,
                                                                             context,
                                                                             databaseId);
        snapshotRepository.save(snapshot);
    }
}
