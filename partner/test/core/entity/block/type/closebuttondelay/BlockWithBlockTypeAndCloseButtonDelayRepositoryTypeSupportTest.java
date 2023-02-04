package ru.yandex.partner.core.entity.block.type.closebuttondelay;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.block.MobileBlockType;
import ru.yandex.partner.core.entity.IncomingFields;
import ru.yandex.partner.core.entity.block.model.MobileRtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockAddServiceImpl;
import ru.yandex.partner.core.entity.block.service.OperationMode;
import ru.yandex.partner.core.entity.block.service.type.add.BlockAddOperationFactory;
import ru.yandex.partner.core.entity.page.model.MobileAppSettings;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.multistate.block.BlockMultistate;

@CoreTest
public class BlockWithBlockTypeAndCloseButtonDelayRepositoryTypeSupportTest {

    @Autowired
    public BlockAddOperationFactory blockAddOperationFactory;
    @Autowired
    private BlockAddServiceImpl blockAddService;
    @Autowired
    PlatformTransactionManager ptm;

    @Test
    void whenNoCloseButtonDelayThenDefaultIsPresent() {
        var block = new MobileRtbBlock()
                .withBlind(0L)
                .withBlockType(MobileBlockType.BANNER.getLiteral())
                .withAdfoxBlock(false)
                .withPageId(43569L)
                .withMultistate(new BlockMultistate());

        Assertions.assertNull(block.getCloseButtonDelay());

        var transactionTemplate = new TransactionTemplate(ptm);

        var blockContainer = blockAddOperationFactory
                .prepareAddOperationContainer(OperationMode.ADD, new IncomingFields());

        blockContainer.getReachablePages()
                .put(
                        MobileRtbBlock.class,
                        blockAddService.getReachablePages(
                                MobileRtbBlock.class,
                                List.of(block),
                                MobileAppSettings.class,
                                CoreFilterNode.neutral()
                        )
                );

        var result = transactionTemplate.execute(ctx ->
                blockAddOperationFactory.createAddOperation(
                        Applicability.PARTIAL,
                        List.of(block),
                        blockContainer
                ).prepareAndApply());

        Long expectedDefault = BlockWithBlockTypeAndCloseButtonDelayModelProviderAndBkFiller
                .DEFAULT_CLOSE_BUTTON_DELAY_VALUE;

        Assertions.assertEquals(expectedDefault, block.getCloseButtonDelay());
    }
}
