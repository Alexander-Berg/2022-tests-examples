/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.wrappedModels;

import com.yandex.datasync.Datatype;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.database.DatabaseManager;
import com.yandex.datasync.internal.model.ValueDto;
import com.yandex.datasync.internal.operation.OperationProcessor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static com.yandex.datasync.asserters.ValueAsserter.assertValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ValuesListTest {

    private static final YDSContext MOCK_CONTEXT = YDSContext.APP;

    private static final String MOCK_DATABASE_ID = "mock_database_id";

    private static final String MOCK_COLLECTION_ID = "mock_collection_id";

    private static final String MOCK_RECORD_ID = "mock_record_id";

    private static final String MOCK_FIELD_ID = "mock_field_id";

    private static final String MOCK_STRING_VALUE = "MOCK_STRING_VALUE";

    @Mock
    private OperationProcessor mockProcessor;

    private DatabaseManager databaseManager;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testSize() throws Exception {
        final List<ValueDto> valuesList = new ArrayList<>();

        final ValueDto value = new ValueDto();
        value.setStringValue(MOCK_STRING_VALUE);
        value.setType(Datatype.STRING);

        valuesList.add(value);

        final ValueDto valueWithList = new ValueDto();
        valueWithList.setListValues(valuesList);

        final ValuesList listWrapper = new ValuesList(databaseManager,
                                                      MOCK_CONTEXT,
                                                      MOCK_DATABASE_ID,
                                                      MOCK_COLLECTION_ID,
                                                      MOCK_RECORD_ID,
                                                      MOCK_FIELD_ID,
                                                      mockProcessor,
                                                      valueWithList);

        assertThat(listWrapper.size(), is(valuesList.size()));
    }

    @Test
    public void testGetValueWrapper() throws Exception {

        final List<ValueDto> valuesList = new ArrayList<>();

        final ValueDto value = new ValueDto();
        value.setStringValue(MOCK_STRING_VALUE);
        value.setType(Datatype.STRING);

        valuesList.add(value);

        final ValueDto valueWithList = new ValueDto();
        valueWithList.setListValues(valuesList);

        final ValuesList listWrapper = new ValuesList(databaseManager,
                                                      MOCK_CONTEXT,
                                                      MOCK_DATABASE_ID,
                                                      MOCK_COLLECTION_ID,
                                                      MOCK_RECORD_ID,
                                                      MOCK_FIELD_ID,
                                                      mockProcessor,
                                                      valueWithList);

        final Value expectedValue = new Value(databaseManager,
                                              MOCK_CONTEXT,
                                              MOCK_DATABASE_ID,
                                              MOCK_COLLECTION_ID,
                                              MOCK_RECORD_ID,
                                              MOCK_FIELD_ID,
                                              mockProcessor,
                                              value);

        assertValue(listWrapper.getValue(0), expectedValue);
    }
}