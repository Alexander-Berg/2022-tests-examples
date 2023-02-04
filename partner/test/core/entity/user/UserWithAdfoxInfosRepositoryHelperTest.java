package ru.yandex.partner.core.entity.user;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.Field;
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

import ru.yandex.partner.core.entity.adfox.model.AdfoxInfo;
import ru.yandex.partner.core.entity.user.type.adfoxinfos.UserWithAdfoxInfosRepositoryHelper;
import ru.yandex.partner.core.mockdataprovider.EmptyDataProvider;
import ru.yandex.partner.dbschema.partner.tables.records.UserAdfoxRecord;

import static ru.yandex.partner.dbschema.partner.tables.UserAdfox.USER_ADFOX;


class UserWithAdfoxInfosRepositoryHelperTest {

    @Test
    void getByUserId() {
        Map<Long, List<AdfoxInfo>> map =
                Map.of(
                        345L, List.of(new AdfoxInfo()
                                .withUserId(345L)
                                .withCreateDate(LocalDateTime.of(2012, 11, 10, 9, 8, 7))
                                .withAdfoxId(987L)
                                .withAdfoxLogin("first_adfox_login")),
                        7656L, List.of(new AdfoxInfo()
                                .withUserId(7656L)
                                .withCreateDate(LocalDateTime.of(2016, 4, 5, 6, 7, 8))
                                .withAdfoxId(545L)
                                .withAdfoxLogin("second_adfox_login"))
                );

        MockProvider provider = new MockProvider(map);
        MockConnection connection = new MockConnection(provider);
        UserWithAdfoxInfosRepositoryHelper adfoxInfoRepositoryJooq =
                new UserWithAdfoxInfosRepositoryHelper(DSL.using(connection, SQLDialect.MYSQL));

        Assertions.assertEquals(map, adfoxInfoRepositoryJooq.getAdfoxInfoByUserIds(map.keySet()));
    }

    @Test
    void getByUserIdEmptyUserIds() {
        UserWithAdfoxInfosRepositoryHelper adfoxInfoRepositoryJooq = new UserWithAdfoxInfosRepositoryHelper(
                Mockito.mock(DSLContext.class));

        Map<Long, List<AdfoxInfo>> results = adfoxInfoRepositoryJooq.getAdfoxInfoByUserIds(Collections.emptyList());
        Map<Long, List<AdfoxInfo>> expected = Collections.emptyMap();
        Assertions.assertEquals(expected, results);
    }

    @Test
    void getByUserIdNotFound() {
        MockConnection connection = new MockConnection(new EmptyDataProvider());
        UserWithAdfoxInfosRepositoryHelper adfoxInfoRepositoryJooq =
                new UserWithAdfoxInfosRepositoryHelper(DSL.using(connection, SQLDialect.MYSQL));

        Map<Long, List<AdfoxInfo>> expected = Collections.emptyMap();
        Assertions.assertEquals(expected, adfoxInfoRepositoryJooq.getAdfoxInfoByUserIds(List.of(23423L)));
    }


    private static class MockProvider implements MockDataProvider {
        private List<Field<?>> fields = List.of(USER_ADFOX.USER_ID, USER_ADFOX.ADFOX_LOGIN,
                USER_ADFOX.ADFOX_ID, USER_ADFOX.CREATE_DATE);
        private Map<Long, List<AdfoxInfo>> map;

        MockProvider(Map<Long, List<AdfoxInfo>> map) {
            this.map = map;
        }

        @Override
        public MockResult[] execute(MockExecuteContext ctx) {
            DSLContext create = DSL.using(SQLDialect.MYSQL);

            Result<UserAdfoxRecord> records = create.newResult(USER_ADFOX);
            for (List<AdfoxInfo> adfoxInfos : map.values()) {
                for (AdfoxInfo adfoxInfo : adfoxInfos) {
                    UserAdfoxRecord record = create.newRecord(USER_ADFOX);
                    record.setUserId(adfoxInfo.getUserId());
                    record.setAdfoxLogin(adfoxInfo.getAdfoxLogin());
                    record.setAdfoxId(adfoxInfo.getAdfoxId());
                    record.setCreateDate(adfoxInfo.getCreateDate());
                    records.add(record);
                }
            }
            return new MockResult[]{new MockResult(records.size(), records)};
        }
    }
}
