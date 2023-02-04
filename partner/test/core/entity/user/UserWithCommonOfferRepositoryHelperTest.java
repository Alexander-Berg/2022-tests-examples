package ru.yandex.partner.core.entity.user;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.Record2;
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

import ru.yandex.partner.core.entity.user.type.commonoffer.UserWithCommonOfferRepositoryHelper;
import ru.yandex.partner.core.mockdataprovider.EmptyDataProvider;

import static ru.yandex.partner.dbschema.partner.tables.CommonOfferAllowedUsers.COMMON_OFFER_ALLOWED_USERS;
import static ru.yandex.partner.dbschema.partner.tables.UserNotifications.USER_NOTIFICATIONS;

class UserWithCommonOfferRepositoryHelperTest {

    @Test
    void getByUserIds() {
        LocalDate now = LocalDate.now();
        Map<Long, Long> expected = Map.of(
                1L, 100L,
                2L, 0L,
                3L, -100L
        );
        Map<Long, LocalDate> map = Map.of(
                1L, now.plusDays(expected.get(1L)),
                2L, now.plusDays(expected.get(2L)),
                3L, now.plusDays(expected.get(3L))
        );
        MockProvider provider = new MockProvider(map);
        MockConnection connection = new MockConnection(provider);
        UserWithCommonOfferRepositoryHelper repository = new UserWithCommonOfferRepositoryHelper(
                DSL.using(connection, SQLDialect.MYSQL)
        );

        Map<Long, Long> result = repository.getCommonOfferByUserIds(map.keySet());
        if (now.equals(LocalDate.now())) {
            Assertions.assertEquals(expected, result);
        } else {
            //на случай, еслитест попали на переход дней
            Assertions.assertEquals(expected, repository.getCommonOfferByUserIds(map.keySet()));
        }
    }

    @Test
    void getByUserIdsEmptyRequest() {
        UserWithCommonOfferRepositoryHelper repository =
                new UserWithCommonOfferRepositoryHelper(Mockito.mock(DSLContext.class));

        Map<Long, Long> expected = Collections.emptyMap();
        Assertions.assertEquals(expected, repository.getCommonOfferByUserIds(Collections.emptyList()));
    }

    @Test
    void getByUserIdsEmptyResult() {
        MockConnection connection = new MockConnection(new EmptyDataProvider());
        UserWithCommonOfferRepositoryHelper repository = new UserWithCommonOfferRepositoryHelper(
                DSL.using(connection, SQLDialect.MYSQL)
        );

        Map<Long, Long> expected = Collections.emptyMap();
        Assertions.assertEquals(expected, repository.getCommonOfferByUserIds(Collections.emptyList()));
    }

    private static class MockProvider implements MockDataProvider {
        private Map<Long, LocalDate> map;

        MockProvider(Map<Long, LocalDate> map) {
            this.map = map;
        }

        @Override
        public MockResult[] execute(MockExecuteContext ctx) {
            DSLContext create = DSL.using(SQLDialect.MYSQL);

            Result<Record2<Long, LocalDate>> records =
                    create.newResult(COMMON_OFFER_ALLOWED_USERS.USER_ID, COMMON_OFFER_ALLOWED_USERS.DEADLINE);

            for (Map.Entry<Long, LocalDate> entry : map.entrySet()) {
                Record2<Long, LocalDate> record2 =
                        create.newRecord(COMMON_OFFER_ALLOWED_USERS.USER_ID, COMMON_OFFER_ALLOWED_USERS.DEADLINE);
                record2.set(USER_NOTIFICATIONS.USER_ID, entry.getKey());
                record2.set(COMMON_OFFER_ALLOWED_USERS.DEADLINE, entry.getValue());

                records.add(record2);
            }

            return new MockResult[]{new MockResult(records.size(), records)};

        }
    }
}
