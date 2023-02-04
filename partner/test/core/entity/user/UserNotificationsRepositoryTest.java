    package ru.yandex.partner.core.entity.user;

import java.util.Collections;
import java.util.List;
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

import ru.yandex.partner.core.entity.notification.NotificationRepository;
import ru.yandex.partner.core.entity.notification.UserNotificationsRepository;
import ru.yandex.partner.core.mockdataprovider.EmptyDataProvider;

import static org.mockito.Mockito.mock;
import static ru.yandex.partner.dbschema.partner.tables.UserNotifications.USER_NOTIFICATIONS;

class UserNotificationsRepositoryTest {

    @Test
    void getUnreadCount() {
        Map<Long, Long> map = Map.of(
                123L, 23434L,
                322L, 3422L
        );
        MockProvider provider = new MockProvider(map);
        MockConnection connection = new MockConnection(provider);
        UserNotificationsRepository notificationRepositoryJooq = new UserNotificationsRepository(
                DSL.using(connection, SQLDialect.MYSQL),
                mock(NotificationRepository.class)
        );

        Assertions.assertEquals(map, notificationRepositoryJooq.getNotificationsCountByUserIds(map.keySet()));
    }

    @Test
    void getUnreadCountEmptyRequest() {
        UserNotificationsRepository notificationRepositoryJooq =
                new UserNotificationsRepository(mock(DSLContext.class), mock(NotificationRepository.class));

        Map<Long, Long> expected = Collections.emptyMap();
        Assertions.assertEquals(expected,
                notificationRepositoryJooq.getNotificationsCountByUserIds(Collections.emptyList()));
    }

    @Test
    void getUnreadCountEmptyResult() {

        MockConnection connection = new MockConnection(new EmptyDataProvider());
        UserNotificationsRepository notificationRepositoryJooq = new UserNotificationsRepository(
                DSL.using(connection, SQLDialect.MYSQL),
                mock(NotificationRepository.class)
        );

        Map<Long, Long> expected = Collections.emptyMap();
        Assertions.assertEquals(expected, notificationRepositoryJooq.getNotificationsCountByUserIds(List.of(123L,
                345L)));
    }

    private static class MockProvider implements MockDataProvider {
        private Map<Long, Long> map;

        MockProvider(Map<Long, Long> map) {
            this.map = map;
        }

        @Override
        public MockResult[] execute(MockExecuteContext ctx) {
            DSLContext create = DSL.using(SQLDialect.MYSQL);

            Result<Record2<Long, Integer>> records = create.newResult(USER_NOTIFICATIONS.USER_ID, DSL.count());
            for (Map.Entry<Long, Long> entry : map.entrySet()) {
                Record2<Long, Integer> record2 = create.newRecord(USER_NOTIFICATIONS.USER_ID, DSL.count());
                record2.set(USER_NOTIFICATIONS.USER_ID, entry.getKey());
                record2.set(DSL.count(), entry.getValue().intValue());

                records.add(record2);
            }

            return new MockResult[]{new MockResult(records.size(), records)};

        }
    }
}
