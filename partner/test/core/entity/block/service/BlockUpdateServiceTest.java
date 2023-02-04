package ru.yandex.partner.core.entity.block.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
import ru.yandex.partner.core.entity.IncomingFields;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockEditFactory;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.BlockWithCommonFields;
import ru.yandex.partner.core.entity.block.model.BlockWithCommonShowVideo;
import ru.yandex.partner.core.entity.block.model.BlockWithStrategy;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.model.prop.BlockWithCommonFieldsCaptionPropHolder;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.junit.MySqlRefresher;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.core.CoreConstants.Strategies.MAX_REVENUE_STRATEGY_ID;
import static ru.yandex.partner.core.CoreConstants.Strategies.MIN_CPM_STRATEGY_ID;
import static ru.yandex.partner.core.CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class BlockUpdateServiceTest extends BaseValidationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockUpdateServiceTest.class);

    @Autowired
    ActionPerformer actionPerformer;

    @Autowired
    BlockTypedRepository repository;

    @Autowired
    private RtbBlockEditFactory rtbBlockEditFactory;

    BlockUpdateServiceTest() {
        super(OperationMode.EDIT);
    }


    @Test
    void createPartialUpdateOperation() {
        List<RtbBlock> blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        LOGGER.info("{}", blocks.get(0).getCaption());
        assertThat(blocks.get(0).getCaption()).isEqualTo("В списке1");

        List<ModelChanges<? super RtbBlock>> modelChanges =
                List.of(ModelChanges.build(347649081345L, RtbBlock.class,
                        BlockWithCommonFieldsCaptionPropHolder.CAPTION, "new_value"));

        var result = updateBlocks(List.of(347649081345L), modelChanges);

        blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getCaption()).isEqualTo("new_value");
    }


    public MassResult<? extends ModelWithId> updateBlocks(
            List<Long> ids, List<ModelChanges<? super RtbBlock>> changes) {
        var action = rtbBlockEditFactory.createAction(changes, new IncomingFields());

        return actionPerformer.doActions(action).getResults().get(RtbBlock.class).iterator().next();
    }

    // TODO: Этот тест проверял валидацию при добавлении некорректной стратегии, но сама валидация расходится с перловой
    // реализацией. Нужно привести в соответствие о потом поправить тест.
/*    @Test
    void updateBlockStrategyFailIncorrect() {
        List<RtbBlock> blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);
        BlockStrategy strategy = new BlockStrategy();

        // MIN_CPM_STRATEGY_ID, MINCPM = null
        strategy.setStrategyType(MIN_CPM_STRATEGY_ID);

        ModelChanges<RtbBlock> modelChanges = ModelChanges.build(347649081345L, RtbBlock.class,
                BlockWithStrategy.STRATEGY, strategy
        );
        MassResult<Long> result = updateBlocks(List.of(modelChanges));

        // Ошибка валидации
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();

        // MIN_CPM_STRATEGY_ID, MINCPM != null, MEDIA_CPM != null
        strategy = new BlockStrategy();
        strategy.setStrategyType(MIN_CPM_STRATEGY_ID);
        strategy.setMincpm(10L);
        strategy.setMediaCpm(10L);

        modelChanges = ModelChanges.build(347649081345L, RtbBlock.class, BlockWithStrategy.STRATEGY, strategy);
        result = updateBlocks(List.of(modelChanges));

        // Ошибка валидации
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();


        // SEPARATE_CPM_STRATEGY_ID, все остальные поля == null
        strategy = new BlockStrategy();
        strategy.setStrategyType(SEPARATE_CPM_STRATEGY_ID);
        modelChanges = ModelChanges.build(347649081345L, RtbBlock.class, BlockWithStrategy.STRATEGY, strategy);
        result = updateBlocks(List.of(modelChanges));

        // Ошибка валидации
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();

        // MAX_REVENUE_STRATEGY_ID, MINCPM != null
        strategy = new BlockStrategy();
        strategy.setStrategyType(MAX_REVENUE_STRATEGY_ID);
        strategy.setMincpm(10L);
        modelChanges = ModelChanges.build(347649081345L, RtbBlock.class, BlockWithStrategy.STRATEGY, strategy);
        result = updateBlocks(List.of(modelChanges));

        // Ошибка валидации
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();

    }*/

    @Test
    void updateBlockStrategyFailEmptyStrategyType() {
        List<RtbBlock> blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getCaption()).isEqualTo("В списке1");

        // Пытаемся установить стратегию с пустым типом
        ModelChanges<RtbBlock> modelChanges = new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(null, RtbBlock.STRATEGY_TYPE);

        var result = updateBlocks(List.of(347649081345L), List.of(modelChanges));
        // Ошибка валидации
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();

        // Пытаемся установить стратегию с неверным типом
        //strategy.setStrategyType(4L);
        ModelChanges<RtbBlock> modelChanges2 = ModelChanges.build(347649081345L, RtbBlock.class,
                BlockWithStrategy.STRATEGY_TYPE, 4L
        );
        var result2 = updateBlocks(List.of(347649081345L), List.of(modelChanges2));
        // Ошибка валидации
        assertThat(result2.getErrorCount()).isOne();
        assertThat(result2.getSuccessfulCount()).isZero();
    }

    @Test
    void updateBlockStrategyNonMincpmNullify() {
        long blockId = 347649081345L;
        List<RtbBlock> blocks = repository.getSafely(List.of(blockId), RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getCaption()).isEqualTo("В списке1");

        List<ModelChanges<? super RtbBlock>> modelChanges = List.of(new ModelChanges<>(blockId, RtbBlock.class)
                .process(BigDecimal.valueOf(3L), RtbBlock.MINCPM)
                .process(MIN_CPM_STRATEGY_ID, RtbBlock.STRATEGY_TYPE)
        );

        var result = updateBlocks(List.of(blockId), modelChanges);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        assertThat(result.getSuccessfulCount()).isOne();

        ModelChanges<RtbBlock> modelChanges2 = new ModelChanges<>(blockId, RtbBlock.class)
                .process(BigDecimal.valueOf(3L), RtbBlock.MINCPM)
                .process(MAX_REVENUE_STRATEGY_ID, RtbBlock.STRATEGY_TYPE);

        result = updateBlocks(List.of(blockId), List.of(modelChanges2));
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        assertThat(result.getSuccessfulCount()).isOne();

        List<RtbBlock> updatedBlock = repository.getSafely(List.of(blockId), RtbBlock.class);
        assertThat(updatedBlock.get(0).getMincpm()).isNull();
        assertThat(updatedBlock.get(0).getStrategyType()).isEqualTo(MAX_REVENUE_STRATEGY_ID);
    }

    @Test
    void updateBlockStrategySuccess() {
        var ids = List.of(347649081345L);
        List<RtbBlock> blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getCaption()).isEqualTo("В списке1");


        ModelChanges<? super RtbBlock> modelChanges = new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(MIN_CPM_STRATEGY_ID, RtbBlock.STRATEGY_TYPE)
                .process(BigDecimal.TEN, RtbBlock.MINCPM);

        var result = updateBlocks(ids, List.of(modelChanges));
        // А здесь всё ок
        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getSuccessfulCount()).isOne();

        blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);

        assertThat(blocks.size()).isEqualTo(1);
        BlockWithStrategy readStrategy = blocks.get(0);
        assertThat(readStrategy).isNotNull();
        assertThat(readStrategy.getStrategyType()).isEqualTo(MIN_CPM_STRATEGY_ID);
        assertThat(readStrategy.getMincpm()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void updateBlockStrategyChangeCoupledFields() {
        List<ModelChanges<? super RtbBlock>> modelChanges = List.of(new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(MIN_CPM_STRATEGY_ID, RtbBlock.STRATEGY_TYPE)
                .process(BigDecimal.TEN, RtbBlock.MINCPM)
        );
        var result = updateBlocks(List.of(347649081345L), modelChanges);
        assertThat(result.getSuccessfulCount()).isOne();

        List<RtbBlock> blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        BlockWithStrategy readStrategy = blocks.get(0);
        assertThat(readStrategy).isNotNull();
        assertThat(readStrategy.getStrategyType()).isEqualTo(MIN_CPM_STRATEGY_ID);
        assertThat(readStrategy.getMincpm()).isEqualByComparingTo(BigDecimal.TEN);


        modelChanges = List.of(new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(MAX_REVENUE_STRATEGY_ID, RtbBlock.STRATEGY_TYPE)
                .process(null, RtbBlock.MINCPM)
        );

        result = updateBlocks(List.of(347649081345L), modelChanges);
        assertThat(result.getSuccessfulCount()).isOne();

        blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        readStrategy = blocks.get(0);
        assertThat(readStrategy).isNotNull();
        assertThat(readStrategy.getStrategyType()).isEqualTo(MAX_REVENUE_STRATEGY_ID);
        assertThat(readStrategy.getMincpm()).isNull();
    }

    @Test
    void updateBlockStrategyChangeGhostedFields() {

        List<ModelChanges<? super RtbBlock>> modelChanges = List.of(new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(SEPARATE_CPM_STRATEGY_ID, RtbBlock.STRATEGY_TYPE)
                .process(true, RtbBlock.MEDIA_ACTIVE)
                .process(BigDecimal.TEN.setScale(3, RoundingMode.HALF_UP), RtbBlock.MEDIA_CPM)
                .process(true, RtbBlock.TEXT_ACTIVE)
                .process(BigDecimal.TEN.setScale(3, RoundingMode.HALF_UP), RtbBlock.TEXT_CPM)
        );
        var result = updateBlocks(List.of(347649081345L), modelChanges);
        assertThat(result.getSuccessfulCount()).isOne();

        List<RtbBlock> blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        BlockWithStrategy readStrategy = blocks.get(0);
        assertThat(readStrategy.getStrategyType()).isEqualTo(SEPARATE_CPM_STRATEGY_ID);
        assertThat(readStrategy.getMediaActive()).isEqualTo(Boolean.TRUE);
        assertThat(readStrategy.getMediaCpm()).isEqualTo(BigDecimal.TEN.setScale(3, RoundingMode.HALF_UP));
        assertThat(readStrategy.getTextActive()).isEqualTo(Boolean.TRUE);
        assertThat(readStrategy.getTextCpm()).isEqualTo(BigDecimal.TEN.setScale(3, RoundingMode.HALF_UP));


        modelChanges = List.of(new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(MAX_REVENUE_STRATEGY_ID, RtbBlock.STRATEGY_TYPE));

        result = updateBlocks(List.of(347649081345L), modelChanges);
        assertThat(result.getSuccessfulCount()).isOne();

        blocks = repository.getSafely(List.of(347649081345L), RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        readStrategy = blocks.get(0);
        assertThat(readStrategy.getStrategyType()).isEqualTo(MAX_REVENUE_STRATEGY_ID);
    }

    @Test
    void updateBlockStrategyFailWithShowVideo() {
        var ids = List.of(347649081345L);
        List<RtbBlock> blocks = repository.getSafely(ids, RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getCaption()).isEqualTo("В списке1");

        // Неверное сочетаение SHOW_VIDEO=false && VIDEO_ACTIVE=true
        ModelChanges<RtbBlock> modelChanges = new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(SEPARATE_CPM_STRATEGY_ID, BlockWithStrategy.STRATEGY_TYPE)
                .process(true, BlockWithStrategy.VIDEO_ACTIVE)
                .process(false, BlockWithCommonShowVideo.SHOW_VIDEO);
        var result = updateBlocks(ids, List.of(modelChanges));
        // Ошибка валидации
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();

        // VIDEO_ACTIVE=null
        modelChanges = new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(MAX_REVENUE_STRATEGY_ID, BlockWithStrategy.STRATEGY_TYPE)
                .process(null, BlockWithStrategy.VIDEO_ACTIVE)
                .process(false, BlockWithCommonShowVideo.SHOW_VIDEO);
        var result2 = updateBlocks(ids, List.of(modelChanges));
        // Должно быть ок
        assertThat(result2.getErrorCount()).isZero();
        assertThat(result2.getSuccessfulCount()).isOne();
    }


    @Test
    void updateBlockStrategyShowVideoToFalse() {
        var ids = List.of(347649081345L);
        ModelChanges<RtbBlock> modelChanges0 = new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(SEPARATE_CPM_STRATEGY_ID, BlockWithStrategy.STRATEGY_TYPE)
                .process(true, BlockWithStrategy.VIDEO_ACTIVE)
                .process(true, BlockWithCommonShowVideo.SHOW_VIDEO);
        updateBlocks(ids, List.of(modelChanges0));


        List<RtbBlock> blocks = repository.getSafely(ids, RtbBlock.class);
        assertThat(blocks.size()).isEqualTo(1);
        assertThat(blocks.get(0).getShowVideo()).isTrue();
        assertThat(blocks.get(0).getVideoActive()).isTrue();
        assertThat(blocks.get(0).getStrategyType()).isEqualTo(3L);


        // Делаем show_video false - video active должен стать null
        ModelChanges<RtbBlock> modelChanges = new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(false, BlockWithCommonShowVideo.SHOW_VIDEO)
                .process(true, RtbBlock.TEXT_ACTIVE);
        var result = updateBlocks(ids, List.of(modelChanges));
        // Должно быть ок
        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getSuccessfulCount()).isOne();

        blocks = repository.getSafely(ids, RtbBlock.class);
        assertThat(blocks.get(0).getVideoActive()).isNull();

    }

    @Test
    void updateToIncorrectPageIdBlockId() {
        ModelChanges<RtbBlock> modelChanges = new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(null, BlockWithCommonFields.BLOCK_ID)
                .process(null, BlockWithCommonFields.PAGE_ID);
        var result = updateBlocks(List.of(347649081345L), List.of(modelChanges));
        // Ошибка валидации
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();
    }

    @Test
    void updateForbiddenPropertiesFail() {
        // Пытаемся обновить запрещённое для обновления поле
        ModelChanges<RtbBlock> modelChanges = ModelChanges.build(347649081345L, RtbBlock.class,
                BaseBlock.PAGE_ID, 100L
        );
        var result = updateBlocks(List.of(347649081345L), List.of(modelChanges));
        // Ошибка валидации
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();
    }

}
