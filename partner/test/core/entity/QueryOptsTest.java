package ru.yandex.partner.core.entity;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.designtemplates.model.DesignTemplates;
import ru.yandex.partner.core.entity.user.filter.UserFilters;
import ru.yandex.partner.core.entity.user.model.User;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.filter.container.DefaultModelFilterContainer;

import static ru.yandex.partner.core.filter.CoreFilterNode.and;

class QueryOptsTest {

    @Test
    void withLimitTest() {
        var opts = (QueryOpts<RtbBlock>) QueryOpts.forClass(RtbBlock.class)
                .withLimit(1000);
        LimitOffset limitOffset = opts.getLimitOffset();

        Assertions.assertNotNull(limitOffset);
        Assertions.assertEquals(1000, limitOffset.limit());
        Assertions.assertEquals(0, limitOffset.offset());
    }

    @Test
    void filterAnd() {
        CoreFilterNode<RtbBlock> filter1 = CoreFilterNode.eq(BlockFilters.CAPTION, "caption");
        CoreFilterNode<RtbBlock> filter2 = CoreFilterNode.in(BlockFilters.ID, List.of(1L, 2L, 3L));

        var opts = (QueryOpts<RtbBlock>) QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.neutral())
                .withFilter(filter1)
                .withFilter(filter2);

        Assertions.assertEquals(
                and(filter1, filter2).toCondition(RtbBlock.class, new DefaultModelFilterContainer<>()),
                ((CoreFilterNode) opts.getFilter()).toCondition(RtbBlock.class, new DefaultModelFilterContainer<>()));
    }

    @Test
    void negativeBatchSizeTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            QueryOpts.forClass(User.class)
                    .withBatch(-2, UserFilters.ID)
                    .withFilter(CoreFilterNode.in(UserFilters.LOGIN, List.of("bla")));
        });

    }

    @Test
    void nextBatchBeforeSpecifySizeThrows() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            QueryOpts.forClass(DesignTemplates.class)
                    .nextBatch(List.of());
        });
    }

    @Test
    void nonIdFilterHolderBatch() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            QueryOpts.forClass(RtbBlock.class)
                    .withBatch(100, BlockFilters.BLOCK_ID);
        });
    }

    @Test
    void batchFilterConditionTest() {
        String actualFilterQuery;
        var opts = (QueryOpts<RtbBlock>) QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.eq(BlockFilters.CAPTION, "bla bla"))
                .withBatch(10, BlockFilters.ID);

        Assertions.assertTrue(opts.isBatchOpts());

        actualFilterQuery = ((CoreFilterNode) opts.getFilter())
                .toCondition(opts.getClazz(), new DefaultModelFilterContainer<>()).toString();
        Assertions.assertTrue(actualFilterQuery.contains("> 0"));
        Assertions.assertNotNull(opts.getLimitOffset());
        Assertions.assertEquals(10, opts.getLimitOffset().limit());

        boolean isLastIdChanged = opts.nextBatch(List.of()); //последняя выборка оказалось пустой
        Assertions.assertFalse(isLastIdChanged);
    }

    @Test
    void defaultParamsTest() {
        var opts = QueryOpts.forClass(User.class);

        Assertions.assertEquals(opts.getClazz(), User.class);
        Assertions.assertEquals(
                CoreFilterNode.<User>neutral().toCondition(User.class, new DefaultModelFilterContainer<>()),
                ((CoreFilterNode) opts.getFilter()).toCondition(User.class, new DefaultModelFilterContainer<>()));

        Assertions.assertNull(opts.getProps());
        Assertions.assertNull(opts.getLimitOffset());
        Assertions.assertNull(opts.getOrderByList());
        Assertions.assertFalse(opts.isForUpdate());
        Assertions.assertFalse(opts.isBatchOpts());
    }

    @Test
    void noArgsForUpdateTrue() {
        var opts = QueryOpts.forClass(DesignTemplates.class)
                .forUpdate();

        Assertions.assertTrue(opts.isForUpdate());
    }
}
