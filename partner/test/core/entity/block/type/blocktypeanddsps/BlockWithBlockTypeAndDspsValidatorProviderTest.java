package ru.yandex.partner.core.entity.block.type.blocktypeanddsps;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.block.MobileBlockType;
import ru.yandex.partner.core.entity.block.container.BlockContainer;
import ru.yandex.partner.core.entity.block.container.BlockContainerImpl;
import ru.yandex.partner.core.entity.block.model.MobileRtbBlock;
import ru.yandex.partner.core.entity.block.service.OperationMode;
import ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds;
import ru.yandex.partner.core.entity.block.type.dsps.BlockWithDspsValidatorProvider;
import ru.yandex.partner.core.entity.dsp.model.Dsp;
import ru.yandex.partner.core.junit.MySqlRefresher;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CoreTest
@ExtendWith(MySqlRefresher.class)
public class BlockWithBlockTypeAndDspsValidatorProviderTest {

    @Autowired
    private BlockWithDspsValidatorProvider blockWithDspsValidatorProvider;

    @Test
    void validateBlockWithCorrectDsp() {
        List<Dsp> dsps = List.of(new Dsp().withId(1L).withUnmoderatedRtbAuction(false));
        BlockContainer blockContainer = BlockContainerImpl.create(OperationMode.ADD);
        blockContainer.setBlockAvailableDsps(Map.of(1L, dsps));

        var result = blockWithDspsValidatorProvider
                .validator(blockContainer)
                .apply(new MobileRtbBlock()
                .withId(1L)
                .withBlockType(MobileBlockType.ADAPTIVE_BANNER.getLiteral())
                .withDsps(dsps));

        var flattenErrors = result.flattenErrors();

        assertEquals(0, flattenErrors.size());
    }

    @Test
    void validateBlockWithIncorrectDsp() {
        List<Dsp> dsps = List.of(new Dsp().withId(2L).withUnmoderatedRtbAuction(false));
        BlockContainer blockContainer = BlockContainerImpl.create(OperationMode.ADD);
        blockContainer.setBlockAvailableDsps(Map.of(1L, dsps));

        var result = blockWithDspsValidatorProvider
                .validator(blockContainer)
                .apply(new MobileRtbBlock()
                .withId(1L)
                .withBlockType(MobileBlockType.ADAPTIVE_BANNER.getLiteral())
                .withDsps(dsps));

        var flattenErrors = result.flattenErrors();

        assertEquals(1, flattenErrors.size());
        assertEquals(BlockDefectIds
                .DspBlocks
                .INVALID_DSP_BLOCK_TYPES, flattenErrors.get(0).getDefect().defectId());
    }

    @Test
    void validateBlockWithTwoDsps() {
        List<Dsp> dsps = List.of(
                new Dsp().withId(1L).withUnmoderatedRtbAuction(false),
                new Dsp().withId(2L).withUnmoderatedRtbAuction(false)
        );
        BlockContainer blockContainer = BlockContainerImpl.create(OperationMode.ADD);
        blockContainer.setBlockAvailableDsps(Map.of(1L, dsps));

        var result = blockWithDspsValidatorProvider
                .validator(blockContainer)
                .apply(new MobileRtbBlock()
                .withId(1L)
                .withBlockType(MobileBlockType.ADAPTIVE_BANNER.getLiteral())
                .withDsps(dsps));

        var flattenErrors = result.flattenErrors();

        assertEquals(1, flattenErrors.size());
        assertEquals(BlockDefectIds.DspBlocks.INVALID_DSP_BLOCK_TYPES, flattenErrors.get(0).getDefect().defectId());
    }
}
