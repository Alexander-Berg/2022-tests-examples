package ru.yandex.partner.core.entity.block.actions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.partner.core.CoreConstants;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.action.exception.DefectInfoWithMsgParams;
import ru.yandex.partner.core.action.exception.presentation.ActionDefectMsg;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.actions.defects.presentation.BlockActionDefectMsg;
import ru.yandex.partner.core.entity.block.actions.rtb.external.RtbBlockEditFactory;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BlockWithMultistate;
import ru.yandex.partner.core.entity.block.model.BlockWithStrategy;
import ru.yandex.partner.core.entity.block.model.Brand;
import ru.yandex.partner.core.entity.block.model.PiCategory;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.block.type.tags.TagService;
import ru.yandex.partner.core.entity.designtemplates.model.DesignTemplates;
import ru.yandex.partner.core.entity.dsp.model.Dsp;
import ru.yandex.partner.core.entity.dsp.service.DspService;
import ru.yandex.partner.core.entity.page.container.PageOperationContainer;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.repository.PageModifyRepository;
import ru.yandex.partner.core.entity.page.repository.PageTypedRepository;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.messages.BlockWithDesignTemplatesMsg;
import ru.yandex.partner.core.multistate.block.BlockStateFlag;
import ru.yandex.partner.core.multistate.designtemplates.DesignTemplatesMultistate;
import ru.yandex.partner.core.multistate.page.ContextPageMultistate;
import ru.yandex.partner.core.multistate.page.PageStateFlag;
import ru.yandex.partner.dbschema.partner.enums.DesignTemplatesType;
import ru.yandex.partner.dbschema.partner.tables.ContextOnSiteRtb;
import ru.yandex.partner.libs.i18n.MsgWithArgs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CoreTest
@ExtendWith(MySqlRefresher.class)
public class BlockActionEditTest {

    private static final int SCALE = CoreConstants.CPM_MAX_SCALE;

    @Autowired
    private ActionPerformer actionPerformer;
    @Autowired
    private RtbBlockEditFactory rtbBlockEditFactory;
    @Autowired
    private BlockService blockService;
    @Autowired
    private TagService tagService;
    @Autowired
    private DspService dspService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private PageTypedRepository pageRepository;
    @Autowired
    private PageModifyRepository pageModifyRepository;
    @Autowired
    private DSLContext dslContext;

    private MessageSourceAccessor messages;

    @BeforeEach
    void setUp() {
        messages = new MessageSourceAccessor(messageSource);
    }

    @Test
    void editDeletedBlock() {
        var ids = List.of(347674247170L);
        var modelChanges = List.of(new ModelChanges<>(347674247170L, RtbBlock.class)
                .process("New caption", RtbBlock.CAPTION));
        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockBefore.getMultistate().test(BlockStateFlag.DELETED)).isTrue();

        var editAction = rtbBlockEditFactory.edit(modelChanges);
        var result = actionPerformer.doActions(editAction);
        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(BlockWithMultistate.MULTISTATE))
        ).get(0);
        assertThat(blockBefore.getMultistate().getEnabledFlags())
                .isEqualTo(blockAfter.getMultistate().getEnabledFlags());
        assertThat(result.isCommitted()).isFalse();
        assertThat(result.getErrors().isEmpty()).isFalse();
        assertEquals(new DefectInfoWithMsgParams(BlockActionDefectMsg.ARCHIVED_BLOCK),
                result.getErrors().get(RtbBlock.class).get(347674247170L).get(0)
                        .getDefectInfo().getDefect().params());
    }

    @Test
    void editBlockSimpleFields() {
        var blockId = 347649081345L;
        var ids = List.of(blockId);

        Mockito.when(tagService.getTagIds()).thenReturn(Set.of(10L, 21L));

        Set<ModelProperty<?, ?>> simpleFields = Set.of(BlockWithMultistate.MULTISTATE,
                RtbBlock.ALT_HEIGHT, RtbBlock.ALT_WIDTH, RtbBlock.ALTERNATIVE_CODE,
                RtbBlock.BK_DATA, RtbBlock.BLIND, RtbBlock.CAPTION, RtbBlock.COMMENT,
                RtbBlock.CUSTOM_BK_OPTIONS, RtbBlock.GEO, RtbBlock.HORIZONTAL_ALIGN,
                RtbBlock.IS_CUSTOM_BK_DATA, RtbBlock.ONLY_PORTAL_TRUSTED_BANNERS,
                RtbBlock.ORDER_TAGS, RtbBlock.READONLY, RtbBlock.SITE_VERSION,
                RtbBlock.TARGET_TAGS);

        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(simpleFields)
        ).get(0);

        assertFalse(blockBefore.getMultistate().test(BlockStateFlag.DELETED));
        assertFalse(blockBefore.getMultistate().test(BlockStateFlag.NEED_UPDATE));

        var editAction = rtbBlockEditFactory.edit(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        // fields in block's table
                        .process(111L, RtbBlock.ALT_HEIGHT)
                        .process(111L, RtbBlock.ALT_WIDTH)
                        .process("alt code", RtbBlock.ALTERNATIVE_CODE)
                        .process(1L, RtbBlock.BLIND)
                        .process("Updated block caption", RtbBlock.CAPTION)
                        .process("Updated block comment", RtbBlock.COMMENT)
                        .process(List.of(1L), RtbBlock.CUSTOM_BK_OPTIONS)
                        .process(List.of(), RtbBlock.GEO)
                        .process(true, RtbBlock.HORIZONTAL_ALIGN)
                        .process(false, RtbBlock.ONLY_PORTAL_TRUSTED_BANNERS)
                        .process(List.of(10L), RtbBlock.ORDER_TAGS)
                        .process(true, RtbBlock.READONLY)
                        .process("mobile", RtbBlock.SITE_VERSION)
                        .process(List.of(21L), RtbBlock.TARGET_TAGS)
                )
        );

        var result = actionPerformer.doActions(editAction);

        assertThat(result.getErrors()).isEmpty();
        assertTrue(result.isCommitted());

        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(simpleFields)
        ).get(0);

        assertThat(blockAfter.getAltHeight()).isEqualTo(111L);
        assertThat(blockAfter.getAltWidth()).isEqualTo(111L);
        assertThat(blockAfter.getAlternativeCode()).isEqualTo("alt code");
        assertThat(blockAfter.getBlind()).isEqualTo(1L);
        assertThat(blockAfter.getCaption()).isEqualTo("Updated block caption");
        assertThat(blockAfter.getComment()).isEqualTo("Updated block comment");
        assertThat(blockAfter.getCustomBkOptions()).isEqualTo(List.of(1L));
        assertThat(blockAfter.getGeo()).isEqualTo(List.of());
        assertTrue(blockAfter.getHorizontalAlign());
        assertThat(blockAfter.getOnlyPortalTrustedBanners()).isFalse();
        assertThat(blockAfter.getOrderTags()).isEqualTo(List.of(10L));
        assertTrue(blockAfter.getReadonly());
        assertThat(blockAfter.getSiteVersion()).isEqualTo("mobile");
        assertThat(blockAfter.getTargetTags()).isEqualTo(List.of(21L));

        var expectedState = blockBefore.getMultistate().copy();
        expectedState.setFlag(BlockStateFlag.NEED_UPDATE, true);
        assertThat(blockAfter.getMultistate()).isEqualTo(expectedState);

        // reset need_update flag
        resetNeedUpdate(blockId);

        var editAction2 = rtbBlockEditFactory.edit(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(true, RtbBlock.IS_CUSTOM_BK_DATA)
                        .process("{}", RtbBlock.BK_DATA)
                )
        );
        var result2 = actionPerformer.doActions(editAction2);

        assertTrue(result2.isCommitted());
        assertTrue(result2.getErrors().isEmpty());

        var blockAfter2 = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(simpleFields)
        ).get(0);
        assertTrue(blockAfter2.getIsCustomBkData());
        assertThat(blockAfter2.getBkData()).isEqualTo("{}");
        assertTrue(blockAfter2.getMultistate().test(BlockStateFlag.NEED_UPDATE));
    }

    private void resetNeedUpdate(long blockId) {
        var curBlock = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, List.of(blockId)))
                .withProps(Set.of(RtbBlock.MULTISTATE))
        ).get(0);

        var resetState = curBlock.getMultistate();
        resetState.setFlag(BlockStateFlag.NEED_UPDATE, false);

        actionPerformer.doActions(rtbBlockEditFactory.edit(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(resetState, RtbBlock.MULTISTATE)
                )
        ));
    }

    private void rejectPage(long pageId) {
        var curPage = pageRepository.getStrictlyFullyFilled(
                List.of(pageId), ContextPage.class, true).get(0);

        var withRejectedState = curPage.getMultistate().copy();
        withRejectedState.setFlag(PageStateFlag.REJECTED, true);

        pageModifyRepository.update(PageOperationContainer.create(), List.of(
                new ModelChanges<>(pageId, ContextPage.class)
                        .process(withRejectedState, ContextPage.MULTISTATE)
                        .applyTo(curPage)
        ));
    }

    @Test
    void editBlockRelatedFields() {
        var blockId = 347649081345L;
        var ids = List.of(blockId);
        Set<ModelProperty<?, ?>> relatedModelFields =
                Set.of(BlockWithMultistate.MULTISTATE, RtbBlock.DSP_BLOCKS, RtbBlock.PI_CATEGORIES, RtbBlock.BRANDS);
        var newMediaSizes = List.of("240x400", "300x300");
        var newPiCategories = List.of(
                new PiCategory().withId(1L).withCpm(BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP)));
        var newBrands = List.of(
                new Brand().withBid(52L).withBlocked(false)
                        .withCpm(BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP)),
                new Brand().withBid(420L).withBlocked(true));

        // related fields in other table
        var editAction3 = rtbBlockEditFactory.edit(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(newMediaSizes, RtbBlock.DSP_BLOCKS)
                        .process(newPiCategories, RtbBlock.PI_CATEGORIES)
                        .process(newBrands, RtbBlock.BRANDS)));

        var result3 = actionPerformer.doActions(editAction3);

        assertThat(result3.getErrors()).isEmpty();
        assertTrue(result3.isCommitted());

        var blockAfter3 = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(relatedModelFields)
        ).get(0);
        assertThat(blockAfter3.getDspBlocks()).isEqualTo(newMediaSizes);
        assertThat(blockAfter3.getPiCategories()).isEqualTo(newPiCategories);
        assertThat(blockAfter3.getBrands()).containsOnlyElementsOf(newBrands);
    }

    @SuppressWarnings("MethodLength")
    @Test
    void editBlockStrategyFields() {
        var blockId = 347649081345L;
        var ids = List.of(blockId);
        Set<ModelProperty<?, ?>> fields =
                Set.of(RtbBlock.MULTISTATE, RtbBlock.STRATEGY_TYPE, RtbBlock.SHOW_VIDEO,
                        RtbBlock.DESIGN_TEMPLATES, RtbBlock.DSPS);
        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        assertEquals(
                CoreConstants.Strategies.MAX_REVENUE_STRATEGY_ID, blockBefore.getStrategyType(),
                "Cannot continue test. Unexpected initial value ");

        // 1 обновляем из стратегии максимальный доход до mincpm стратегии
        resetNeedUpdate(blockId);

        doActionEditAndAssertIfError(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(CoreConstants.Strategies.MIN_CPM_STRATEGY_ID, BlockWithStrategy.STRATEGY_TYPE)
                        .process(BigDecimal.valueOf(50L), BlockWithStrategy.MINCPM)));

        var blockAfter1 = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        assertTrue(blockAfter1.getMultistate().test(BlockStateFlag.NEED_UPDATE));
        assertThat(blockAfter1.getStrategyType()).isEqualTo(CoreConstants.Strategies.MIN_CPM_STRATEGY_ID);
        assertThat(blockAfter1.getMincpm()).isEqualTo(BigDecimal.valueOf(50L).setScale(SCALE, RoundingMode.HALF_UP));


        // 2 обновлем от mincpm стратегии до максимальный доход
        resetNeedUpdate(blockId);
        doActionEditAndAssertIfError(List.of(
                new ModelChanges<>(blockId, RtbBlock.class)
                        .process(CoreConstants.Strategies.MAX_REVENUE_STRATEGY_ID, BlockWithStrategy.STRATEGY_TYPE)));

        var blockAfter2 = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        assertTrue(blockAfter2.getMultistate().test(BlockStateFlag.NEED_UPDATE));
        assertThat(blockAfter2.getStrategyType()).isEqualTo(CoreConstants.Strategies.MAX_REVENUE_STRATEGY_ID);

        // 3 обновляем от стратегии максимальный доход до раздельный cpm
        resetNeedUpdate(blockId);
        doActionEditAndAssertIfError(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID, BlockWithStrategy.STRATEGY_TYPE)
                        .process(true, BlockWithStrategy.MEDIA_ACTIVE)
                        .process(false, BlockWithStrategy.MEDIA_BLOCKED)
                        .process(BigDecimal.valueOf(50L), BlockWithStrategy.MEDIA_CPM)
                        .process(true, BlockWithStrategy.TEXT_ACTIVE)
                        .process(false, BlockWithStrategy.TEXT_BLOCKED)
                        .process(BigDecimal.valueOf(60L), BlockWithStrategy.TEXT_CPM)));


        var blockAfter3 = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        assertTrue(blockAfter3.getMultistate().test(BlockStateFlag.NEED_UPDATE));
        assertThat(blockAfter3.getStrategyType()).isEqualTo(CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID);
        assertThat(blockAfter3.getMediaActive()).isEqualTo(true);
        assertThat(blockAfter3.getMediaBlocked()).isEqualTo(false);
        assertThat(blockAfter3.getMediaCpm()).isEqualTo(BigDecimal.valueOf(50L).setScale(SCALE, RoundingMode.HALF_UP));
        assertThat(blockAfter3.getTextActive()).isEqualTo(true);
        assertThat(blockAfter3.getTextBlocked()).isEqualTo(false);
        assertThat(blockAfter3.getTextCpm()).isEqualTo(BigDecimal.valueOf(60L).setScale(SCALE, RoundingMode.HALF_UP));


        // 4 обновляем от стратегии раздельный cpm без видео до раздельный cpm с видео

        resetNeedUpdate(blockId);
        doActionEditAndAssertIfError(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID, BlockWithStrategy.STRATEGY_TYPE)
                        .process(true, BlockWithStrategy.MEDIA_ACTIVE)
                        .process(false, BlockWithStrategy.MEDIA_BLOCKED)
                        .process(BigDecimal.valueOf(50L), BlockWithStrategy.MEDIA_CPM)
                        .process(true, BlockWithStrategy.TEXT_ACTIVE)
                        .process(false, BlockWithStrategy.TEXT_BLOCKED)
                        .process(BigDecimal.valueOf(60L), BlockWithStrategy.TEXT_CPM)
                        .process(true, BlockWithStrategy.VIDEO_ACTIVE)
                        .process(false, BlockWithStrategy.VIDEO_BLOCKED)
                        .process(BigDecimal.valueOf(70L), BlockWithStrategy.VIDEO_CPM)
                        .process(true, RtbBlock.SHOW_VIDEO)));


        var blockAfter4 = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        assertTrue(blockAfter4.getMultistate().test(BlockStateFlag.NEED_UPDATE));
        assertThat(blockAfter4.getStrategyType()).isEqualTo(CoreConstants.Strategies.SEPARATE_CPM_STRATEGY_ID);
        assertThat(blockAfter4.getMediaActive()).isEqualTo(true);
        assertThat(blockAfter4.getMediaBlocked()).isEqualTo(false);
        assertThat(blockAfter4.getMediaCpm()).isEqualTo(BigDecimal.valueOf(50L).setScale(SCALE, RoundingMode.HALF_UP));
        assertThat(blockAfter4.getTextActive()).isEqualTo(true);
        assertThat(blockAfter4.getTextBlocked()).isEqualTo(false);
        assertThat(blockAfter4.getTextCpm()).isEqualTo(BigDecimal.valueOf(60L).setScale(SCALE, RoundingMode.HALF_UP));
        assertThat(blockAfter4.getVideoActive()).isEqualTo(true);
        assertThat(blockAfter4.getVideoBlocked()).isEqualTo(false);
        assertThat(blockAfter4.getVideoCpm()).isEqualTo(BigDecimal.valueOf(70L).setScale(SCALE, RoundingMode.HALF_UP));


        assertEquals(new DesignTemplates()
                        .withId(13395349L)
                        .withMultistate(new DesignTemplatesMultistate())
                        .withCaption(messages.getMessage(BlockWithDesignTemplatesMsg.DEFAULT_VIDEO_DESIGN))
                        .withType(DesignTemplatesType.video)
                        .withPageId(blockAfter4.getPageId())
                        .withBlockId(blockAfter4.getBlockId())
                        .withDesignSettings(Map.of("name", "inpage"))
                        .withUpdateTime(blockAfter4.getDesignTemplates().get(2).getUpdateTime()),
                blockAfter4.getDesignTemplates().get(2),
                "default video design should be added"
        );

        assertThat(blockAfter4.getDsps())
                .as("video dsp should be added")
                .anySatisfy(dsp ->
                        assertThat(dsp.getTypes()).isEqualTo(List.of(CoreConstants.DspTypes.DSP_VIDEO.typeId))
                );


        // 5 обновляем от стратегии максимальный доход до максимальный доход

        resetNeedUpdate(blockId);
        doActionEditAndAssertIfError(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(CoreConstants.Strategies.MAX_REVENUE_STRATEGY_ID, BlockWithStrategy.STRATEGY_TYPE)
                        .process(false, RtbBlock.SHOW_VIDEO)
                )
        );

        var blockAfter5 = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        assertTrue(blockAfter5.getMultistate().test(BlockStateFlag.NEED_UPDATE));
        assertThat(blockAfter5.getStrategyType()).isEqualTo(CoreConstants.Strategies.MAX_REVENUE_STRATEGY_ID);
        assertThat(blockAfter5.getDesignTemplates())
                .as("video design should be removed, only tga + media present")
                .hasSize(2);
        assertThat(blockAfter5.getDsps())
                .as("video dsp should be removed")
                .allMatch(dsp -> !(
                        dsp.getTypes().size() == 1 && dsp.getTypes().contains(CoreConstants.DspTypes.DSP_VIDEO)));

        var dspsSizeAfter5 = blockAfter5.getDsps().size();

        // 6. show_video to true changes designs and should set need_update
        resetNeedUpdate(blockId);
        doActionEditAndAssertIfError(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        // intentional empty dsps
                        .process(List.of(), RtbBlock.DSPS)
                        .process(true, RtbBlock.SHOW_VIDEO)
                )
        );

        var blockAfter6 = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        assertTrue(blockAfter6.getMultistate().test(BlockStateFlag.NEED_UPDATE));
        assertThat(blockAfter6.getDsps().size())
                .as("video dsps should appear on show_video = true")
                .isGreaterThan(dspsSizeAfter5);
    }

    @Test
    void editBlockDesignsWithInvalidPageState() {
        var blockId = 347649081345L;
        var ids = List.of(blockId);
        Set<ModelProperty<?, ?>> fields = Set.of(RtbBlock.MULTISTATE, RtbBlock.DESIGN_TEMPLATES); // RtbBlock.STRATEGY,
        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        var allDsps = dspService.getAllNotDeletedDsps();
        // video dsp for validation
        var dspDirect = getDsp(allDsps, CoreConstants.DSP_DIRECT_ID);

        resetNeedUpdate(blockId);
        doActionEditAndAssertIfError(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(true, RtbBlock.SHOW_VIDEO)
                        .process(List.of(dspDirect), RtbBlock.DSPS)
                )
        );

        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        ).get(0);

        assertTrue(blockAfter.getMultistate().test(BlockStateFlag.NEED_UPDATE));

        blockAfter.getDesignTemplates().forEach(
                template -> template.withCaption("Some new caption")
        );

        rejectPage(blockAfter.getPageId());
        resetNeedUpdate(blockId);

        var actionEdit = rtbBlockEditFactory.edit(List.of(new ModelChanges<>(blockId, RtbBlock.class)
                .process(blockAfter.getDesignTemplates(), RtbBlock.DESIGN_TEMPLATES)
        ));

        var result = actionPerformer.doActions(actionEdit);

        assertThat(result.getErrors())
                .containsKey(RtbBlock.class);
        assertThat(result.getErrors()
                .get(RtbBlock.class)
                .get(blockAfter.getId())
                .get(0)
                .getDefectInfo()
                .getDefect()
                .params()
        ).isEqualTo(new DefectInfoWithMsgParams(MsgWithArgs.of(ActionDefectMsg.CAN_NOT_DO_ACTION, "edit")));

        assertFalse(result.isCommitted(), "failed to update");
    }

    private void doActionEditAndAssertIfError(Collection<ModelChanges<RtbBlock>> modelChanges) {
        var actionEdit = rtbBlockEditFactory.edit(modelChanges);

        var result = actionPerformer.doActions(actionEdit);

        assertThat(result.getErrors()).as("should not have errors").isEmpty();
        assertTrue(result.isCommitted(), "failed to update");
    }

    @Test
    void captionEditCaption() {
        var blockId = 347649081345L;
        var ids = List.of(blockId);

        Mockito.when(tagService.getTagIds()).thenReturn(Set.of(10L, 21L));

        var blockBefore = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
        ).get(0);


        var rememberedDsps = new ArrayList<>(blockBefore.getDsps());

        assertFalse(blockBefore.getMultistate().hasFlag(BlockStateFlag.DELETED));
        assertFalse(blockBefore.getMultistate().hasFlag(BlockStateFlag.NEED_UPDATE));

        var editAction = rtbBlockEditFactory.edit(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process("updated caption", RtbBlock.CAPTION)
                )
        );

        var result = actionPerformer.doActions(editAction);

        assertTrue(result.isCommitted());
        assertTrue(result.getErrors().isEmpty());

        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
        ).get(0);

        var expectedState = blockBefore.getMultistate().copy();
        expectedState.setFlag(BlockStateFlag.NEED_UPDATE, true);

        assertThat(blockAfter)
                .isEqualTo(blockBefore
                        .withCaption("updated caption")
                        .withMultistate(expectedState)
                        .withCampaign(blockBefore.getCampaign()
                                .withUpdateTime(blockAfter.getCampaign().getUpdateTime())
                                .withMultistate(new ContextPageMultistate(List.of(PageStateFlag.NEED_UPDATE)))
                        )
                );

        assertThat(blockAfter.getDsps()).isEqualTo(rememberedDsps);
    }

    @Test
    void editDspsTest() {
        var blockId = 347649081345L;
        var ids = List.of(blockId);
        Set<ModelProperty<?, ?>> fields = Set.of(RtbBlock.ID, RtbBlock.DSPS, RtbBlock.DSPS_UNMODERATED,
                RtbBlock.DSP_MODE);

        var allDsps = dspService.getAllNotDeletedDsps();
        var dsp2317563 = getDsp(allDsps, 2317563L);
        var dsp2563081 = getDsp(allDsps, 2563081L); // unmoderated
        var dsp2563120 = getDsp(allDsps, 2563120L); // unmoderated
        var dspForUpdate = List.of(dsp2317563, dsp2563081);
        var dspUnmoderatedForUpdate = List.of(2563120L);

        var editAction = rtbBlockEditFactory.edit(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(dspForUpdate, RtbBlock.DSPS)
                        .process(dspUnmoderatedForUpdate, RtbBlock.DSPS_UNMODERATED)
                        .process("whitelist", RtbBlock.DSP_MODE)
                )
        );

        var result = actionPerformer.doActions(editAction);

        assertThat(result.getErrors()).as("failed to update").isEmpty();
        assertTrue(result.isCommitted(), "failed to update");

        var block = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(fields)
        );

        assertEquals(
                dspForUpdate.stream().map(Dsp::getId).collect(Collectors.toSet()),
                block.get(0).getDsps().stream().map(Dsp::getId).collect(Collectors.toSet())
        );
        assertEquals(
                dspUnmoderatedForUpdate.stream()
                        .sorted().collect(Collectors.toList()),
                block.get(0).getDspsUnmoderated().stream()
                        .sorted().collect(Collectors.toList())
        );
        assertEquals("whitelist", block.get(0).getDspMode());

        var editActionWithError = rtbBlockEditFactory.edit(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(List.of(), RtbBlock.DSPS)
                        .process("blacklist", RtbBlock.DSP_MODE)
                )
        );
        var resultWithError = actionPerformer.doActions(editActionWithError);
        assertThat(resultWithError.getErrors()).as("empty dsps blacklist: expecting validation error").isNotEmpty();
        assertFalse(resultWithError.isCommitted(), "empty dsps blacklist: unexpected ok update");

        var editActionWithError2 = rtbBlockEditFactory.edit(
                List.of(new ModelChanges<>(blockId, RtbBlock.class)
                        .process(List.of(), RtbBlock.DSPS)
                        .process("whitelist", RtbBlock.DSP_MODE)
                )
        );

        var resultWithError2 = actionPerformer.doActions(editActionWithError2);

        assertThat(resultWithError2.getErrors()).as("empty dsps whitelist: expecting validation error").isNotEmpty();
        assertFalse(resultWithError2.isCommitted(), "empty dsps whitelist:  unexpected ok update");
    }

    private Dsp getDsp(Collection<Dsp> allDsps, long id) {
        var dsp = allDsps.stream().filter(dsp1 -> id == dsp1.getId()).findAny();
        if (dsp.isEmpty()) {
            Assertions.fail("Cannot find Dsp with id = " + id);
        }

        return dsp.get();
    }

    @Test
    void editDesignTemplates() {
        var ids = List.of(347649081345L);
        var designTemplate = new DesignTemplates()
                .withPageId(41443L)
                .withBlockId(1L)
                .withCaption("new_template")
                .withDesignSettings(Map.of("name", "adaptive0418",
                        "limit", 1,
                        "width", "756",
                        "height", 250))
                .withType(DesignTemplatesType.tga)
                .withCustomFormatDirect(false)
                .withMultistate(null);

        var modelChanges = List.of(new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(List.of(designTemplate), RtbBlock.DESIGN_TEMPLATES)
                .process("mobile", RtbBlock.SITE_VERSION));

        var editAction = rtbBlockEditFactory.edit(modelChanges);
        var result = actionPerformer.doActions(editAction);
        assertThat(result.isCommitted()).isTrue();
        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
                .withProps(Set.of(RtbBlock.DESIGN_TEMPLATES))
        ).get(0);
        var newDesign = blockAfter.getDesignTemplates().get(0);
        assertThat(newDesign.getDesignSettings().get("width")).isEqualTo(756);
    }

    @Test
    void editShowVideo() {
        var ids = List.of(347649081345L);
        dslContext.update(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB)
                .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.SHOW_VIDEO, 1L)
                .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.VIDEO_ACTIVE, 0L)
                .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.VIDEO_BLOCKED, 0L)
                .set(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.VIDEO_CPM, BigDecimal.valueOf(70L))
                .where(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.UNIQUE_ID.eq(347649081345L))
                .execute();

        var modelChanges = List.of(new ModelChanges<>(347649081345L, RtbBlock.class)
                .process(false, RtbBlock.SHOW_VIDEO));
        var editAction = rtbBlockEditFactory.edit(modelChanges);
        var result = actionPerformer.doActions(editAction);
        assertThat(result.isCommitted()).isTrue();
        var blockAfter = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, ids))
        ).get(0);
        assertThat(blockAfter.getVideoActive()).isNull();
        assertThat(blockAfter.getVideoBlocked()).isNull();
        assertThat(blockAfter.getVideoCpm()).isNull();

    }

}
