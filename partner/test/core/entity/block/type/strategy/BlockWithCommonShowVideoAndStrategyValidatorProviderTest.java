package ru.yandex.partner.core.entity.block.type.strategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.CoreConstants;
import ru.yandex.partner.core.entity.block.model.BlockWithCommonShowVideoAndStrategy;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds;
import ru.yandex.partner.core.entity.block.service.validation.defects.StrategyDefectParams;
import ru.yandex.partner.core.entity.block.type.commonshowvideoandstrategy.BlockWithCommonShowVideoAndStrategyValidatorProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockWithCommonShowVideoAndStrategyValidatorProviderTest {

    private BlockWithCommonShowVideoAndStrategyValidatorProvider validatorProvider;

    @BeforeEach
    void setUp() {
        validatorProvider = new BlockWithCommonShowVideoAndStrategyValidatorProvider();
    }

  /*  @Test
    void emptyStrategy() {

        var validationResult = validatorProvider.validator().apply(new RtbBlock());

        assertEquals(List.of(new Defect(BlockDefectIds.Strategy.EMPTY_STRATEGY)), getDefects(validationResult));
    }*/

    @Test
    void emptyStrategyType() {

        var validationResult =
                validatorProvider.validator().apply(new RtbBlock());

        assertEquals(List.of(new Defect(DefectIds.CANNOT_BE_NULL)), getDefects(validationResult));
    }

    @Test
    void incorrectMincpm() {

        var validationResult = validatorProvider.validator().apply(new RtbBlock()
                .withStrategyType(CoreConstants.Strategies.MIN_CPM_STRATEGY_ID)
        );

        assertEquals(List.of(new Defect(BlockDefectIds.Strategy.MINCPM_VALUE_RANGE,
                        new StrategyDefectParams().withMaxCpm(BigDecimal.valueOf(9999L)))),
                getDefects(validationResult));
    }

    @Test
    void incorrectStrategyType() {
        var validationResult = validatorProvider.validator().apply(new RtbBlock()
                .withStrategyType(11111L)
        );

        assertEquals(List.of(new Defect(BlockDefectIds.Strategy.INCORRECT_STRATEGY_VALUE)),
                getDefects(validationResult));
    }

    @Test
    void noActiveAds() {

        var validationResult = validatorProvider.validator().apply(new RtbBlock()
                .withStrategyType(CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID)
        );

        assertEquals(List.of(new Defect(BlockDefectIds.Strategy.NOT_ACTIVE_ADS)), getDefects(validationResult));
    }

    @Test
    void allAdsBlocked() {

        var validationResult = validatorProvider.validator().apply(new RtbBlock()
              .withStrategyType(CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID)
                        .withMediaActive(true).withMediaBlocked(true)
                        .withTextActive(true).withTextBlocked(true)
        );

        assertEquals(List.of(new Defect(BlockDefectIds.Strategy.ALL_ADS_BLOCKED)), getDefects(validationResult));
    }

    @Test
    void videoNotAllowed() {
        var validationResult = validatorProvider.validator().apply(new RtbBlock()
                .withStrategyType(CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID)
                        .withMediaActive(true).withMediaBlocked(false).withMediaCpm(BigDecimal.TEN)
                        .withVideoActive(true)
        );

        assertEquals(List.of(new Defect(DefectIds.MUST_BE_NULL)), getDefects(validationResult));
    }

    @Test
    void success() {

        var validationResult = validatorProvider.validator().apply(new RtbBlock().withShowVideo(true)
                .withStrategyType(CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID)
                        .withMediaActive(true).withMediaBlocked(false).withMediaCpm(BigDecimal.TEN)
                        .withVideoActive(true).withVideoBlocked(true)
                );

        assertEquals(List.of(), getDefects(validationResult));
    }

    private List<Defect> getDefects(ValidationResult<BlockWithCommonShowVideoAndStrategy, Defect> validationResult) {
        return validationResult.flattenErrors().stream().map(DefectInfo::getDefect).collect(Collectors.toList());
    }
}
