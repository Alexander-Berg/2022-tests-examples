package ru.yandex.partner.core.entity.block.actions.rtb;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.block.MobileBlockType;
import ru.yandex.partner.core.entity.IncomingFields;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.rtb.mobile.internal.InternalMobileRtbBlockEditFactory;
import ru.yandex.partner.core.entity.block.model.InternalMobileRtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockAddServiceImpl;
import ru.yandex.partner.core.entity.block.service.OperationMode;
import ru.yandex.partner.core.entity.block.service.type.add.BlockAddOperationFactory;
import ru.yandex.partner.core.entity.page.filter.PageFilters;
import ru.yandex.partner.core.entity.page.model.InternalMobileApp;
import ru.yandex.partner.core.entity.page.service.PageService;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.block.BlockMultistate;
import ru.yandex.partner.core.multistate.page.InternalMobileAppMultistate;

import static ru.yandex.partner.core.multistate.page.PageStateFlag.NEED_UPDATE;
import static ru.yandex.partner.core.multistate.page.PageStateFlag.PROTECTED;

@ExtendWith(MySqlRefresher.class)
@CoreTest
public class ProtectedInternalMobileRtbTest {
    @Autowired
    PlatformTransactionManager ptm;
    @Autowired
    BlockAddOperationFactory blockAddOperationFactory;
    @Autowired
    BlockAddServiceImpl blockAddService;
    @Autowired
    InternalMobileRtbBlockEditFactory internalMobileRtbBlockEditFactory;
    @Autowired
    ActionPerformer actionPerformer;
    @Autowired
    PageService pageService;

    @Test
    void whenDoBlockEditActionThenPageNeedUpdateFlagEnabled() {
        InternalMobileApp pageBefore = pageService.findAll(QueryOpts.forClass(InternalMobileApp.class)
                .withFilter(PageFilters.PAGE_ID.eq(810L))
        ).get(0);
        Assertions.assertEquals(
                new InternalMobileAppMultistate(List.of(PROTECTED)),
                pageBefore.getMultistate()
        );

        var blockToAdd = new InternalMobileRtbBlock()
                .withBlockType(MobileBlockType.BANNER.getLiteral())
                .withPageId(810L)
                .withCaption("initial caption")
                .withMultistate(new BlockMultistate());
        var transactionTemplate = new TransactionTemplate(ptm);

        var blockContainer = blockAddOperationFactory
                .prepareAddOperationContainer(OperationMode.ADD, new IncomingFields());
        blockContainer.getReachablePages()
                .put(
                        InternalMobileRtbBlock.class,
                        blockAddService.getReachablePages(
                                InternalMobileRtbBlock.class,
                                List.of(blockToAdd),
                                InternalMobileApp.class,
                                CoreFilterNode.neutral()
                        )
                );
        var id = transactionTemplate.execute(ctx ->
                blockAddOperationFactory.createAddOperation(
                        Applicability.PARTIAL,
                        List.of(blockToAdd),
                        blockContainer
                ).prepareAndApply())
                .getResult().get(0)
                .getResult();

        var actionEdit = internalMobileRtbBlockEditFactory.edit(
                List.of(new ModelChanges<>(id, InternalMobileRtbBlock.class)
                        .process("edited caption", InternalMobileRtbBlock.CAPTION)
                )
        );

        var result = actionPerformer.doActions(actionEdit);
        Assertions.assertTrue(result.isCommitted());

        InternalMobileApp pageAfter = pageService.findAll(QueryOpts.forClass(InternalMobileApp.class)
                .withFilter(PageFilters.PAGE_ID.eq(810L))
        ).get(0);
        Assertions.assertEquals(
                new InternalMobileAppMultistate(List.of(PROTECTED, NEED_UPDATE)),
                pageAfter.getMultistate()
        );
    }
}
