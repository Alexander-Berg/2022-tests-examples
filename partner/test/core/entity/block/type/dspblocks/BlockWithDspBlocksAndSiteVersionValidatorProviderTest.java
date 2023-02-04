package ru.yandex.partner.core.entity.block.type.dspblocks;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.validation.defect.ids.StringDefectIds;
import ru.yandex.direct.validation.defect.params.StringDefectParams;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.entity.block.model.BlockWithDspBlocksAndSiteVersion;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds;
import ru.yandex.partner.core.entity.block.service.validation.defects.DspBlocksDefectParams;
import ru.yandex.partner.core.entity.block.type.commonshowvideoandstrategy.SiteVersionType;
import ru.yandex.partner.core.entity.block.type.dspblocksandsiteversion.BlockWithDspBlocksAndSiteVersionValidatorProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockWithDspBlocksAndSiteVersionValidatorProviderTest {
    private BlockWithDspBlocksAndSiteVersionValidatorProvider validatorProvider;

    @BeforeEach
    void setUp() {
        validatorProvider = new BlockWithDspBlocksAndSiteVersionValidatorProvider();
    }

    @Test
    void nullOrEmpty() {
        var validationResult = validatorProvider.validator().apply(new RtbBlock());

        assertEquals(List.of(new Defect<>(BlockDefectIds.DspBlocks.INVALID_DSP_BLOCK_TYPES)),
                getDefects(validationResult));

        validationResult = validatorProvider.validator().apply(new RtbBlock().withDspBlocks(List.of()));
        assertEquals(List.of(new Defect<>(BlockDefectIds.DspBlocks.INVALID_DSP_BLOCK_TYPES)),
                getDefects(validationResult));
    }

    @Test
    void tooLongDspBlock() {
        var validationResult = validatorProvider.validator().apply(new RtbBlock().withDspBlocks(List.of(
                "123456x123456"
        )));
        assertEquals(List.of(
                new Defect<>(StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX,
                        new StringDefectParams().withMaxLength(12))
                ),
                getDefects(validationResult));
    }

    @Test
    void duplicatesDspBlocks() {
        var validationResult = validatorProvider.validator().apply(new RtbBlock().withDspBlocks(List.of(
                "100x100",
                "100x150",
                "100x200",
                "100x100",
                "100x150"
        )));
        assertEquals(List.of(
                new Defect<>(BlockDefectIds.DspBlocks.HAS_DUPLICATES,
                        new DspBlocksDefectParams().withDuplicates(Set.of("100x100", "100x150")))
                ),
                getDefects(validationResult));
    }

    @Test
    void badDspBlocks() {
        // 1
        var dspBlocks = List.of("100x100", "300x400");
        var validationResult = validatorProvider.validator().apply(
                new RtbBlock().withSiteVersion("test_site_version").withDspBlocks(dspBlocks));

        assertEquals(List.of(
                new Defect(BlockDefectIds.DspBlocks.BAD_DSP_BLOCKS,
                        new DspBlocksDefectParams().withBadBlocks(new HashSet<>(dspBlocks)))
                ),
                getDefects(validationResult)
        );

        // 2
        dspBlocks = List.of("1000x120", "160x600", "100x150", "240x600", "200x100", "240x400");

        validationResult = validatorProvider.validator().apply(
                new RtbBlock().withSiteVersion(SiteVersionType.TURBO_DESKTOP.getLiteral()).withDspBlocks(dspBlocks));
        assertEquals(List.of(
                new Defect(BlockDefectIds.DspBlocks.BAD_DSP_BLOCKS,
                        new DspBlocksDefectParams().withBadBlocks(Set.of("100x150", "200x100")))
                ),
                getDefects(validationResult)
        );
    }

    @Test
    void success() {
        var validationResult = validatorProvider.validator().apply(new RtbBlock()
                .withSiteVersion(SiteVersionType.TURBO_DESKTOP.getLiteral())
                .withDspBlocks(List.of(
                        "1000x120",
                        "160x600",
                        "240x400"
                )));
        assertEquals(List.of(), getDefects(validationResult));
    }


    private List<Defect> getDefects(ValidationResult<BlockWithDspBlocksAndSiteVersion, Defect> validationResult) {
        return validationResult.flattenErrors().stream().map(DefectInfo::getDefect).collect(Collectors.toList());
    }
}
