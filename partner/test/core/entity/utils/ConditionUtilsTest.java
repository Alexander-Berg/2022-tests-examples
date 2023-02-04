package ru.yandex.partner.core.entity.utils;

import java.util.Collection;
import java.util.List;

import org.jooq.Condition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.PageBlockIds;

import static ru.yandex.partner.dbschema.partner.tables.ContextOnSiteRtb.CONTEXT_ON_SITE_RTB;

public class ConditionUtilsTest {
    private static final Long PAGE_ID = 1234L;
    private static final Long BLOCK_ID_FOR_PAGE_ID = 1L;
    private static final Long BLOCK_ID2_FOR_PAGE_ID = 2L;

    private static final Long PAGE_ID2 = 6789L;
    private static final Long BLOCK_ID_FOR_PAGE_ID2 = 3L;

    @Test
    void checkSimpleCondition() {
        Collection<PageBlockIds> pageBlockIds = List.of(new PageBlockIds(PAGE_ID, BLOCK_ID_FOR_PAGE_ID));
        Condition condition = ConditionUtils.toPageBlockCondition(pageBlockIds, CONTEXT_ON_SITE_RTB.CAMPAIGN_ID,
                CONTEXT_ON_SITE_RTB.ID);

        Assertions.assertEquals(condition.toString(), "(\n" +
                "  \"partner\".\"context_on_site_rtb\".\"campaign_id\" = 1234\n" +
                "  and \"partner\".\"context_on_site_rtb\".\"id\" in (1)\n" +
                ")");
    }

    @Test
    void checkComplexCondition() {
        Collection<PageBlockIds> pageBlockIds = List.of(
                new PageBlockIds(PAGE_ID, BLOCK_ID_FOR_PAGE_ID),
                new PageBlockIds(PAGE_ID, BLOCK_ID2_FOR_PAGE_ID),
                new PageBlockIds(PAGE_ID2, BLOCK_ID_FOR_PAGE_ID2)
        );

        Condition condition = ConditionUtils.toPageBlockCondition(pageBlockIds, CONTEXT_ON_SITE_RTB.CAMPAIGN_ID,
                CONTEXT_ON_SITE_RTB.ID);

        Assertions.assertEquals(condition.toString(), "(\n" +
                "  (\n" +
                "    \"partner\".\"context_on_site_rtb\".\"campaign_id\" = 1234\n" +
                "    and \"partner\".\"context_on_site_rtb\".\"id\" in (\n" +
                "      1, 2\n" +
                "    )\n" +
                "  )\n" +
                "  or (\n" +
                "    \"partner\".\"context_on_site_rtb\".\"campaign_id\" = 6789\n" +
                "    and \"partner\".\"context_on_site_rtb\".\"id\" in (3)\n" +
                "  )\n" +
                ")");
    }
}
