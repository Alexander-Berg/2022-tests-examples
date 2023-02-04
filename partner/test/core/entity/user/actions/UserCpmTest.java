package ru.yandex.partner.core.entity.user.actions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.user.actions.factories.UserEditFactory;
import ru.yandex.partner.core.entity.user.filter.UserFilters;
import ru.yandex.partner.core.entity.user.model.User;
import ru.yandex.partner.core.entity.user.service.UserService;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.dbschema.partner.tables.Queue;
import ru.yandex.partner.defaultconfiguration.PartnerLocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MySqlRefresher.class)
@CoreTest
public class UserCpmTest {
    @Autowired
    private ActionPerformer actionPerformer;

    @Autowired
    private UserEditFactory userEditFactory;

    @Autowired
    private UserService userService;

    @Autowired
    private DSLContext dslContext;

    @Test
    void changeNextCurrency() throws JSONException {
        var queueIdsBefore = dslContext.select(Queue.QUEUE.ID).from(Queue.QUEUE).fetchSet(Queue.QUEUE.ID);
        var localDateTimeBefore = PartnerLocalDateTime.now().minusSeconds(1L);

        var userId = 1009L;
        var currency = "USD";
        var rate = new BigDecimal("11.129");
        UserActionEdit userActionEdit = userEditFactory.edit(List.of(
                new ModelChanges<>(userId, User.class)
                        .process(currency, User.NEXT_CURRENCY)
                        .process(rate, User.CURRENCY_RATE)
        ));

        var result = actionPerformer.doActions(userActionEdit);

        assertTrue(result.isCommitted());

        var users = userService.findAll(QueryOpts.forClass(User.class)
                .withFilter(CoreFilterNode.in(UserFilters.ID, List.of(userId)))
                .withProps(Set.of(User.NEXT_CURRENCY))
        );

        assertEquals(currency, users.get(0).getNextCurrency());
        assertEquals(rate, users.get(0).getCurrencyRate());

        var records = dslContext.selectFrom(Queue.QUEUE).where(Queue.QUEUE.ID.notIn(queueIdsBefore)).fetch();

        assertEquals(1, records.size());

        var record = records.get(0);
        assertTrue(record.getAddDt().isAfter(localDateTimeBefore));
        assertEquals(10L, record.getMethodType());
        JSONAssert.assertEquals(
                "{\"currency\":\"USD\",\"user\":1009,\"currency_rate\":11.129}", record.getParams(), false
        );
        assertEquals(0L, record.getUserId());
        assertEquals(0L, record.getMultistate());
        assertEquals(0L, record.getTries());
    }
}
