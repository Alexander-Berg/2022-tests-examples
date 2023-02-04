package ru.yandex.partner.core.action;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockDeleteFactory;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.dbschema.partner.Tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.partner.core.CoreConstants.SYSTEM_CRON_USER_ID;

@CoreTest
@ExtendWith(MySqlRefresher.class)
public class ActionUserIdContextTest {
    private static final Long SOME_NOT_DELETED_BLOCK_ID = 347649081345L;
    private static final Long SOME_EXISTING_USER_ID = 1010L;

    @Autowired
    ActionUserIdContext actionUserIdContext;
    @Autowired
    RtbBlockDeleteFactory rtbBlockDeleteFactory;
    @Autowired
    ActionPerformer actionPerformer;
    @Autowired
    DSLContext dslContext;
    @Autowired
    BlockService blockService;


    @Test
    void userIdInLogShouldEqualsCronIdIfNotSet() {
        var action = rtbBlockDeleteFactory.delete(List.of(SOME_NOT_DELETED_BLOCK_ID));
        actionPerformer.doActions(action);

        Long insertedUserId = getInsertedId();

        assertEquals(SYSTEM_CRON_USER_ID, insertedUserId);
    }

    @Test
    void userIdInLogShouldEqualsProvidedId() {
        try {
            actionUserIdContext.setUserId(SOME_EXISTING_USER_ID); // it usually happens in ActionUserIdSettingFilter
            var action = rtbBlockDeleteFactory.delete(List.of(SOME_NOT_DELETED_BLOCK_ID));
            actionPerformer.doActions(action);

            Long insertedUserId = getInsertedId();

            assertEquals(SOME_EXISTING_USER_ID, insertedUserId);
        } finally {
            actionUserIdContext.setDefault();
        }
    }

    private Long getInsertedId() {
        return dslContext.selectFrom(Tables.CONTEXT_ON_SITE_RTB_ACTION_LOG)
                .orderBy(Tables.CONTEXT_ON_SITE_RTB_ACTION_LOG.ID.desc())
                .limit(1)
                .fetchOne()
                .getUserId();
    }
}
