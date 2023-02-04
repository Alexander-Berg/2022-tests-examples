package ru.yandex.partner.core.entity.user;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.partner.core.entity.user.type.features.UserWithFeaturesRepositoryHelper;
import ru.yandex.partner.core.mockdataprovider.EmptyDataProvider;
import ru.yandex.partner.dbschema.partner.tables.records.UserFeaturesRecord;

import static ru.yandex.partner.dbschema.partner.tables.UserFeatures.USER_FEATURES;

class UserWithFeaturesRepositoryHelperTest {
    private static final long TEST_ID = 9194L;

    @Test
    void getFeatures() {
        Map<Long, List<String>> map = Map.of(
                111L, List.of("feature_1"),
                222L, List.of("feature_2", "feature_3")
        );
        MockDataProvider provider = new MockProvider(map);
        MockConnection connection = new MockConnection(provider);
        UserWithFeaturesRepositoryHelper featureRepositoryJooq = new UserWithFeaturesRepositoryHelper(
                DSL.using(connection, SQLDialect.MYSQL));

        Assertions.assertEquals(map, featureRepositoryJooq.groupByIds(map.keySet()));
    }

    @Test
    void getFeaturesEmptyUserIds() {
        UserWithFeaturesRepositoryHelper featureRepositoryJooq =
                new UserWithFeaturesRepositoryHelper(Mockito.mock(DSLContext.class));

        Map<Long, List<String>> results = featureRepositoryJooq.groupByIds(Collections.emptyList());
        Map<Long, List<String>> expected = Collections.emptyMap();
        Assertions.assertEquals(expected, results);
    }

    @Test
    void getFeaturesEmptyResult() {
        MockDataProvider provider = new EmptyDataProvider();
        MockConnection connection = new MockConnection(provider);
        UserWithFeaturesRepositoryHelper featureRepositoryJooq = new UserWithFeaturesRepositoryHelper(
                DSL.using(connection, SQLDialect.MYSQL));

        Map<Long, List<String>> results = featureRepositoryJooq.groupByIds(List.of(TEST_ID));
        Map<Long, List<String>> expected = Collections.emptyMap();
        Assertions.assertEquals(expected, results);
    }


    private static class MockProvider implements MockDataProvider {

        private final Map<Long, List<String>> map;

        MockProvider(Map<Long, List<String>> map) {
            this.map = map;
        }

        @Override
        public MockResult[] execute(MockExecuteContext ctx) {
            DSLContext create = DSL.using(SQLDialect.MYSQL);

            Result<UserFeaturesRecord> result = create.newResult(USER_FEATURES);

            for (Map.Entry<Long, List<String>> entry : map.entrySet()) {
                for (String s : entry.getValue()) {
                    UserFeaturesRecord record2 = create.newRecord(USER_FEATURES);
                    record2.set(USER_FEATURES.USER_ID, entry.getKey());
                    record2.set(USER_FEATURES.FEATURE, s);
                    result.add(record2);
                }
            }

            return new MockResult[]{new MockResult(result.size(), result)};
        }
    }
}
