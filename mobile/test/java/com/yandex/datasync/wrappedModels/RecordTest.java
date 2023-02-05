/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.wrappedModels;

import androidx.annotation.NonNull;

import com.squareup.moshi.Moshi;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.model.ValueDto;
import com.yandex.datasync.internal.model.response.FieldDto;
import com.yandex.datasync.internal.model.response.RecordDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.internal.operation.OperationProcessor;
import com.yandex.datasync.util.ResourcesUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.yandex.datasync.asserters.ValueAsserter.assertValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("ConstantConditions")
public class RecordTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private static final String MOCK_COLLECTION_ID = "sport";

    private static final String MOCK_RECORD_ID = "monday";

    private static final String MOCK_FIELD_ID = "starts";

    private static final String INVALID_FIELD_ID = "invalid_field_id";

    @Mock
    private OperationProcessor mockProcessor;

    private DatabaseManager databaseManager;

    private Record record;

    private RecordDto recordDto;

    @Before
    public void setUp() throws IOException {
        initMocks(this);

        final String jsonString = ResourcesUtil.getTextFromFile("database_snapshot.json");
        final SnapshotResponse snapshot =
                new Moshi.Builder().build().adapter(SnapshotResponse.class)
                        .fromJson(jsonString);
        recordDto = getRecord(snapshot, MOCK_COLLECTION_ID, MOCK_RECORD_ID);

        record = new Record(databaseManager,
                            MOCK_CONTEXT,
                            MOCK_DATABASE_ID,
                            MOCK_COLLECTION_ID,
                            MOCK_RECORD_ID,
                            mockProcessor,
                            recordDto);
    }

    @Test
    public void testHasField() throws Exception {
        assertTrue(record.hasField(MOCK_FIELD_ID));
        assertFalse(record.hasField(INVALID_FIELD_ID));
    }

    @Test
    public void testGetValueWrapper() throws Exception {
        final Value actual = record.getValue(MOCK_FIELD_ID);

        final Value expected = new Value(databaseManager,
                                         MOCK_CONTEXT,
                                         MOCK_DATABASE_ID,
                                         MOCK_COLLECTION_ID,
                                         MOCK_RECORD_ID,
                                         MOCK_FIELD_ID,
                                         mockProcessor,
                                         getValue(recordDto, MOCK_FIELD_ID));

        assertValue(actual, expected);
    }

    @Test
    public void testGetFieldsIds() throws Exception {
        final List<String> fieldIds = Arrays.asList(record.getFieldsIds());

        assertThat(fieldIds.size(), is(recordDto.getFields().size()));

        for (final FieldDto field : recordDto.getFields()) {
            assertTrue(fieldIds.contains(field.getFieldId()));
        }
    }

    private RecordDto getRecord(@NonNull final SnapshotResponse snapshot,
                                @NonNull final String collectionId,
                                @NonNull final String recordId) {
        for (final RecordDto record : snapshot.getRecords().getItems()) {
            if (collectionId.equals(record.getCollectionId())
                && recordId.equals(record.getRecordId())) {
                return record;
            }
        }
        return new RecordDto();
    }

    private ValueDto getValue(@NonNull final RecordDto record,
                              @NonNull final String fieldId) {
        for (final FieldDto field : record.getFields()) {
            if (fieldId.equals(field.getFieldId())) {
                return field.getValue();
            }
        }
        return new ValueDto();
    }
}