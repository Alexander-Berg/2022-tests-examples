package ru.yandex.partner.core.entity.assistant;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.assistant.repository.AssistantRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.partner.dbschema.partner.tables.Assistants.ASSISTANTS;

public class AssistantRepositoryTest {

    public static final long USER_ID = 123L;

    @Test
    public void testGetPageIdsBuUserId() {
        List<Long> pageIds = List.of(123L, 234L, 345L);
        MockProvider mockProvider = new MockProvider(pageIds);

        MockConnection connection = new MockConnection(mockProvider);
        AssistantRepository assistantRepository = new AssistantRepository(DSL.using(connection, SQLDialect.MYSQL));

        assertEquals(pageIds, assistantRepository.getPageIdsForUserId(USER_ID));
    }

    private static class MockProvider implements MockDataProvider {

        private List<Long> pageIds;

        MockProvider(List<Long> ownerIds) {
            this.pageIds = ownerIds;
        }

        @Override
        public MockResult[] execute(MockExecuteContext ctx) {
            DSLContext create = DSL.using(SQLDialect.MYSQL);

            assertEquals("select distinct `partner`.`assistants`.`page_id` " +
                    "from `partner`.`assistants` where `partner`.`assistants`.`user_id` = ?", ctx.sql());
            assertEquals(USER_ID, ctx.bindings()[0]);

            Result<Record1<Long>> records = create.newResult(ASSISTANTS.PAGE_ID);
            records.addAll(pageIds.stream().map(pageId -> {
                Record1<Long> record = create.newRecord(ASSISTANTS.PAGE_ID);
                record.set(ASSISTANTS.PAGE_ID, pageId);
                return record;
            }).collect(Collectors.toList()));
            return new MockResult[]{new MockResult(records.size(), records)};
        }
    }
}
