package ru.yandex.partner.core.mockdataprovider;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;

public class EmptyDataProvider implements MockDataProvider {
    @Override
    public MockResult[] execute(MockExecuteContext ctx) {
        DSLContext create = DSL.using(SQLDialect.MYSQL);
        return new MockResult[]{new MockResult(0, create.newResult())};
    }
}
