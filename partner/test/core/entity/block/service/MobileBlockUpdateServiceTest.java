package ru.yandex.partner.core.entity.block.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelWithId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.block.MobileBlockType;
import ru.yandex.partner.core.entity.IncomingFields;
import ru.yandex.partner.core.entity.block.actions.rtb.mobile.external.MobileRtbBlockEditFactory;
import ru.yandex.partner.core.entity.block.model.BlockWithStrategy;
import ru.yandex.partner.core.entity.block.model.MobileRtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.junit.MySqlRefresher;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.core.CoreConstants.Strategies.MAX_REVENUE_STRATEGY_ID;
import static ru.yandex.partner.core.CoreConstants.Strategies.MIN_CPM_STRATEGY_ID;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class MobileBlockUpdateServiceTest extends BaseValidationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MobileBlockUpdateServiceTest.class);

    @Autowired
    ActionPerformer actionPerformer;

    @Autowired
    BlockTypedRepository repository;

    @Autowired
    private MobileRtbBlockEditFactory rtbBlockEditFactory;

    MobileBlockUpdateServiceTest() {
        super(OperationMode.EDIT);
    }



    public MassResult<? extends ModelWithId> updateBlocks(
            List<Long> ids, List<ModelChanges<? super MobileRtbBlock>> changes) {
        return updateBlocks(ids, changes, new IncomingFields());
    }
    private MassResult<? extends ModelWithId> updateBlocks(
            List<Long> ids, List<ModelChanges<? super MobileRtbBlock>> changes, IncomingFields incomingFields) {
        var action = rtbBlockEditFactory.createAction(changes, incomingFields);

        return actionPerformer.doActions(action).getResults().get(MobileRtbBlock.class).iterator().next();
    }

    @Test
    void updateMobileBlockIsBidding() {
        var ids = List.of(72058885715787778L);
        ModelChanges<MobileRtbBlock> modelChanges0 = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(MIN_CPM_STRATEGY_ID, BlockWithStrategy.STRATEGY_TYPE)
                .process(BigDecimal.TEN, BlockWithStrategy.MINCPM);
        updateBlocks(ids, List.of(modelChanges0));


        List<MobileRtbBlock> blocks = repository.getSafely(ids, MobileRtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getStrategyType()).isEqualTo(MIN_CPM_STRATEGY_ID);
        assertThat(blocks.get(0).getMincpm()).isEqualTo(BigDecimal.TEN.setScale(3, RoundingMode.HALF_UP));


        // is_bidding -> true - mincpm должен стать null, а стратегия - max revenue
        ModelChanges<MobileRtbBlock> modelChanges = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(true, MobileRtbBlock.IS_BIDDING);
        var result = updateBlocks(ids, List.of(modelChanges));
        // Должно быть ок
        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getSuccessfulCount()).isOne();

        blocks = repository.getSafely(ids, MobileRtbBlock.class);
        assertThat(blocks.get(0).getStrategyType()).isEqualTo(MAX_REVENUE_STRATEGY_ID);
        assertThat(blocks.get(0).getMincpm()).isNull();

    }

    @Test
    void updateMobileBlockRewarded() {
        var ids = List.of(72058885715787778L);
        ModelChanges<MobileRtbBlock> modelChanges0 = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(MobileBlockType.REWARDED.getLiteral(), MobileRtbBlock.BLOCK_TYPE)
                .process("test", MobileRtbBlock.CURRENCY_TYPE)
                .process(10, MobileRtbBlock.CURRENCY_VALUE)
                .process("test", MobileRtbBlock.SIGN)
                .process("test", MobileRtbBlock.CALLBACK);
        updateBlocks(ids, List.of(modelChanges0));


        List<MobileRtbBlock> blocks = repository.getSafely(ids, MobileRtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getCurrencyType()).isEqualTo("test");
        assertThat(blocks.get(0).getCurrencyValue()).isEqualTo(10);
        assertThat(blocks.get(0).getCallback()).isEqualTo("test");
        assertThat(blocks.get(0).getSign()).isEqualTo("test");




        ModelChanges<MobileRtbBlock> modelChanges = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(MobileBlockType.BANNER.getLiteral(), MobileRtbBlock.BLOCK_TYPE);
        var result = updateBlocks(ids, List.of(modelChanges));
        // Должно быть ок
        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getSuccessfulCount()).isOne();

        blocks = repository.getSafely(ids, MobileRtbBlock.class);
        assertThat(blocks.get(0).getCurrencyType()).isNull();
        assertThat(blocks.get(0).getCurrencyValue()).isNull();
        assertThat(blocks.get(0).getCallback()).isNull();
        assertThat(blocks.get(0).getSign()).isNull();
    }

    @Test
    void updateMobileBlockType() {
        var ids = List.of(72058885715787778L);
        ModelChanges<MobileRtbBlock> modelChanges0 = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(MobileBlockType.NATIVE.getLiteral(), MobileRtbBlock.BLOCK_TYPE);
        updateBlocks(ids, List.of(modelChanges0));

        List<MobileRtbBlock> blocks = repository.getSafely(ids, MobileRtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getBlockType()).isEqualTo(MobileBlockType.NATIVE.getLiteral());

        ModelChanges<MobileRtbBlock> modelChanges = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(null, MobileRtbBlock.BLOCK_TYPE);
        var result = updateBlocks(ids, List.of(modelChanges));
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();

    }

    @Test
    void updateMobileBlockNative() {
        var ids = List.of(72058885715787778L);
        IncomingFields incomingFields = new IncomingFields();
        ModelChanges<MobileRtbBlock> modelChanges0 = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(MobileBlockType.NATIVE.getLiteral(), MobileRtbBlock.BLOCK_TYPE)
                .process(Boolean.TRUE, MobileRtbBlock.SHOW_SLIDER)
                .process(9, MobileRtbBlock.LIMIT);
        incomingFields.addUpdatedFields(Set.of(MobileRtbBlock.BLOCK_TYPE.name(),
                MobileRtbBlock.SHOW_SLIDER.name(),
                MobileRtbBlock.LIMIT.name()));
        updateBlocks(ids, List.of(modelChanges0), incomingFields);
        List<MobileRtbBlock> blocks = repository.getSafely(ids, MobileRtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getShowSlider()).isEqualTo(Boolean.TRUE);
        assertThat(blocks.get(0).getLimit()).isEqualTo(9);


        incomingFields = new IncomingFields();
        ModelChanges<MobileRtbBlock> modelChanges = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(Boolean.FALSE, MobileRtbBlock.SHOW_SLIDER);
        incomingFields.addUpdatedFields(Set.of(MobileRtbBlock.BLOCK_TYPE.name(),
                MobileRtbBlock.SHOW_SLIDER.name()));
        updateBlocks(ids, List.of(modelChanges), incomingFields);

        blocks = repository.getSafely(ids, MobileRtbBlock.class);
        assertThat(blocks.get(0).getShowSlider()).isEqualTo(Boolean.FALSE);
        assertThat(blocks.get(0).getLimit()).isNull();

        incomingFields = new IncomingFields();
        modelChanges = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(Boolean.TRUE, MobileRtbBlock.SHOW_SLIDER);
        incomingFields.addUpdatedFields(Set.of(MobileRtbBlock.BLOCK_TYPE.name(),
                MobileRtbBlock.SHOW_SLIDER.name()));
        updateBlocks(ids, List.of(modelChanges), incomingFields);

        blocks = repository.getSafely(ids, MobileRtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getShowSlider()).isEqualTo(Boolean.TRUE);
        assertThat(blocks.get(0).getLimit()).isEqualTo(1);


        incomingFields = new IncomingFields();
        modelChanges = new ModelChanges<>(72058885715787778L, MobileRtbBlock.class)
                .process(Boolean.TRUE, MobileRtbBlock.SHOW_SLIDER)
                .process(null, MobileRtbBlock.LIMIT);
        var result = updateBlocks(ids, List.of(modelChanges), incomingFields);
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();


    }



}
