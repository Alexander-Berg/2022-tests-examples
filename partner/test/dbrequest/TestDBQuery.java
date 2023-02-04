package ru.yandex.partner.coreexperiment.dbrequest;

import org.jooq.SortField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.dbschema.partner.Tables.USERS;

public class TestDBQuery {
    @Test
    void checkDBQuery() throws Exception {
        DBQuery query = new DBQuery()
                .setCondition(USERS.ID.eq(100500L))
                .setOrderFields(USERS.LOGIN.asc())
                .setLimit(10)
                .setOffset(5);

        assertThat(query.getCondition()).isEqualTo(USERS.ID.eq(100500L));

        SortField<?>[] arrayFields = {USERS.LOGIN.asc()};
        assertThat(query.getOrderFields()).isEqualTo(arrayFields);

        assertThat(query.getLimit()).isEqualTo(10);
        assertThat(query.getOffset()).isEqualTo(5);
    }
}
