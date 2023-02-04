package ru.yandex.partner.core.entity.block.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.block.MobileBlockType;
import ru.yandex.partner.core.configuration.TestOperationContainerConfigurer;
import ru.yandex.partner.core.entity.IncomingFields;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.container.BlockContainer;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.BlockWithMultistate;
import ru.yandex.partner.core.entity.block.model.BlockWithStrategy;
import ru.yandex.partner.core.entity.block.model.MobileRtbBlock;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.service.type.add.BlockAddOperationFactory;
import ru.yandex.partner.core.entity.designtemplates.model.CommonDesignTemplates;
import ru.yandex.partner.core.entity.page.filter.PageFilters;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.service.PageService;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.filter.operator.FilterOperator;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.block.BlockStateFlag;
import ru.yandex.partner.core.operation.container.OperationContainerConfigurer;
import ru.yandex.partner.dbschema.partner.enums.ContextOnSiteRtbModel;
import ru.yandex.partner.dbschema.partner.tables.ContextOnSiteRtb;
import ru.yandex.partner.defaultconfiguration.PartnerLocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.partner.core.CoreConstants.Strategies.MAX_REVENUE_STRATEGY_ID;
import static ru.yandex.partner.core.CoreConstants.Strategies.MIN_CPM_STRATEGY_ID;
import static ru.yandex.partner.core.entity.page.service.validation.defects.PageDefectIds.ContextFieldDefectIds.LIMIT_EXCEEDED;
import static ru.yandex.partner.dbschema.partner.tables.Pages.PAGES;


@ExtendWith(MySqlRefresher.class)
@CoreTest
class BlockAddOperationTest {
    @Autowired
    BlockAddOperationFactory addOperationFactory;
    @Autowired
    BlockService blockService;
    @Autowired
    BlockAddService<BlockWithMultistate> blockAddService;
    @Autowired
    PageService pageService;
    @Autowired
    PlatformTransactionManager ptm;
    @Autowired
    DSLContext dslContext;
    @Autowired
    TestOperationContainerConfigurer<BlockContainer> testOperationContainerConfigurer;

    @Test
    void addBlockMobile() {
        var incomingFields = new IncomingFields();
        Multimap<Model, ModelProperty<?, ?>> data
                = Multimaps.newSetMultimap(new IdentityHashMap<>(), HashSet::new);
        var modelProps = new HashSet<>(MobileRtbBlock.allModelProperties());
        modelProps.remove(MobileRtbBlock.MULTISTATE);
        modelProps.remove(MobileRtbBlock.CREATE_DATE);
        modelProps.remove(MobileRtbBlock.ID);
        var block = blockService.findAll(QueryOpts.forClass(MobileRtbBlock.class)
                .withFilter(BlockFilters.ID.eq(72060312483790849L))
        ).get(0);
        block.setBlockId(null);
        block.setMultistate(null);
        block.setCreateDate(null);
        block.setId(null);
        block.setCustomBkOptions(null);
        block.setHorizontalAlign(null);
        block.setShowVideo(false);
        block.setStrategyType(MIN_CPM_STRATEGY_ID);
        block.setMincpm(BigDecimal.TEN);
        block.setIsBidding(true);
        block.setCurrencyType("test");
        block.setSign("test");
        block.setCallback("test");
        block.setCurrencyValue(10);
        data.putAll(block, modelProps);

        var strategyProperties = new HashSet<>(BlockWithStrategy.allModelProperties());
        data.putAll(block, strategyProperties);

        var designTemplatesProps = new HashSet<>(CommonDesignTemplates.allModelProperties());
        designTemplatesProps.remove(CommonDesignTemplates.MULTISTATE);

        incomingFields.addIncomingFields(data);

        var countBlockBefore = blockService.count(QueryOpts.forClass(MobileRtbBlock.class)
                .withFilter(CoreFilterNode.neutral()));
        var container = addOperationFactory.prepareAddOperationContainer(
                OperationMode.ADD, incomingFields);
        container.getReachablePages().put(MobileRtbBlock.class, getPagesMobile(List.of(block)));
        container.setCanValidateDesignAsManager(Map.of(block.getClass(), true));

        var transactionTemplate = new TransactionTemplate(ptm);

        var result = transactionTemplate.execute(ctx ->
                addOperationFactory.createAddOperation(Applicability.FULL, List.of(block), container)
                        .prepareAndApply());
        var newUniqId = result.getResult().get(0).getResult();

        assertThat(newUniqId).isNotNull();

        List<MobileRtbBlock> insertedBlocks = blockService.findAll(QueryOpts.forClass(MobileRtbBlock.class)
                .withFilter(BlockFilters.ID.eq(newUniqId))
        );
        var curInsertedBlock = insertedBlocks.get(0);

        assertThat(curInsertedBlock.getMobileAppMode()).isEqualTo(0);
        assertThat(curInsertedBlock.getStrategyType()).isEqualTo(MAX_REVENUE_STRATEGY_ID);
        assertThat(curInsertedBlock.getMincpm()).isNull();
        assertThat(curInsertedBlock.getCurrencyValue()).isEqualTo(10);
        assertThat(curInsertedBlock.getCurrencyType()).isEqualTo("test");
        assertThat(curInsertedBlock.getSign()).isEqualTo("test");
        assertThat(curInsertedBlock.getCallback()).isEqualTo("test");
    }

    @Test
    void addBlockTestReward() {
        var incomingFields = new IncomingFields();
        Multimap<Model, ModelProperty<?, ?>> data
                = Multimaps.newSetMultimap(new IdentityHashMap<>(), HashSet::new);
        var modelProps = new HashSet<>(MobileRtbBlock.allModelProperties());
        modelProps.remove(MobileRtbBlock.MULTISTATE);
        modelProps.remove(MobileRtbBlock.CREATE_DATE);
        modelProps.remove(MobileRtbBlock.ID);
        var block = blockService.findAll(QueryOpts.forClass(MobileRtbBlock.class)
                .withFilter(BlockFilters.ID.eq(72060312483790849L))
        ).get(0);
        block.setBlockId(null);
        block.setMultistate(null);
        block.setCreateDate(null);
        block.setId(null);
        block.setCustomBkOptions(null);
        block.setHorizontalAlign(null);
        block.setShowVideo(false);
        block.setStrategyType(MIN_CPM_STRATEGY_ID);
        block.setMincpm(BigDecimal.TEN);
        block.setIsBidding(true);
        block.setCurrencyType("test");
        block.setSign("test");
        block.setCallback("test");
        block.setCurrencyValue(10);
        block.setBlockType(MobileBlockType.BANNER.getLiteral());
        data.putAll(block, modelProps);

        incomingFields.addIncomingFields(data);

        var countBlockBefore = blockService.count(QueryOpts.forClass(MobileRtbBlock.class)
                .withFilter(CoreFilterNode.neutral()));
        var container = addOperationFactory.prepareAddOperationContainer(
                OperationMode.ADD, incomingFields);
        container.getReachablePages().put(MobileRtbBlock.class, getPagesMobile(List.of(block)));
        container.setCanValidateDesignAsManager(Map.of(block.getClass(), true));

        var transactionTemplate = new TransactionTemplate(ptm);

        var result = transactionTemplate.execute(ctx ->
                addOperationFactory.createAddOperation(Applicability.FULL, List.of(block), container)
                        .prepareAndApply());
        var newUniqId = result.getResult().get(0).getResult();

        assertThat(newUniqId).isNotNull();

        List<MobileRtbBlock> insertedBlocks = blockService.findAll(QueryOpts.forClass(MobileRtbBlock.class)
                .withFilter(BlockFilters.ID.eq(newUniqId))
        );
        var curInsertedBlock = insertedBlocks.get(0);

        assertThat(curInsertedBlock.getCurrencyValue()).isNull();
        assertThat(curInsertedBlock.getCurrencyType()).isNull();
        assertThat(curInsertedBlock.getSign()).isNull();
        assertThat(curInsertedBlock.getCallback()).isNull();
    }


    @Test
    void addBlock() {
        var incomingFields = new IncomingFields();
        Multimap<Model, ModelProperty<?, ?>> data
                = Multimaps.newSetMultimap(new IdentityHashMap<>(), HashSet::new);
        var modelProps = new HashSet<>(RtbBlock.allModelProperties());
        modelProps.remove(RtbBlock.MULTISTATE);
        modelProps.remove(RtbBlock.CREATE_DATE);
        modelProps.remove(RtbBlock.ID);
        var block = prepareBlock();
        data.putAll(block, modelProps);

        var strategyProperties = new HashSet<>(BlockWithStrategy.allModelProperties());
        data.putAll(block, strategyProperties);

        var designTemplatesProps = new HashSet<>(CommonDesignTemplates.allModelProperties());
        designTemplatesProps.remove(CommonDesignTemplates.MULTISTATE);

        block.getDesignTemplates().forEach(it -> data.putAll(it, designTemplatesProps));

        incomingFields.addIncomingFields(data);

        var testDateTime = PartnerLocalDateTime.now();

        var countBlockBefore = blockService.count(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.neutral()));
        var container = addOperationFactory.prepareAddOperationContainer(
                OperationMode.ADD, incomingFields);
        container.getReachablePages().put(RtbBlock.class, getPages(List.of(block)));
        container.setCanValidateDesignAsManager(Map.of(block.getClass(), true));

        var transactionTemplate = new TransactionTemplate(ptm);

        var result = transactionTemplate.execute(ctx ->
                addOperationFactory.createAddOperation(Applicability.FULL, List.of(block), container)
                        .prepareAndApply());
        var newUniqId = result.getResult().get(0).getResult();
        var countBlockAfter = blockService.count(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.neutral()));
        assertThat(countBlockAfter - countBlockBefore).isEqualTo(1L);

        List<RtbBlock> insertedBlocks = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.eq(BlockFilters.ID, newUniqId))
        );
        var curInsertedBlock = insertedBlocks.get(0);

        assertThat(block.getDsps()).isNotNull();
        assertThat(block.getDsps().size()).isGreaterThan(0);
        assertThat(block.getDesignTemplates().size()).isEqualTo(curInsertedBlock.getDesignTemplates().size());
        assertThat(block.getBrands().size()).isEqualTo(curInsertedBlock.getBrands().size());
        assertThat(curInsertedBlock.getCreateDate().toLocalDate()).isAfterOrEqualTo(testDateTime.toLocalDate());
        //проверяем что дефолты установился
        assertThat(curInsertedBlock.getCustomBkOptions()).isNotNull();
        assertThat(curInsertedBlock.getHorizontalAlign()).isTrue();
        // проверяем что поле модели установилось
        var modelField = dslContext.select(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.MODEL)
                .from(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB)
                .where(ContextOnSiteRtb.CONTEXT_ON_SITE_RTB.UNIQUE_ID.eq(curInsertedBlock.getId()))
                .fetchOne();
        assertThat(modelField.value1()).isEqualTo(ContextOnSiteRtbModel.context_on_site_rtb);
        //зануляем некоторые элементы для сравнения
        curInsertedBlock.getBrands().forEach(it -> it.setId(null)); //у эталона еще нет id

        curInsertedBlock.getDesignTemplates().forEach(it -> {
            //DesignSettings Map и эквивалентно, если этот тот же объект
            it.setDesignSettings(null);
            it.setUpdateTime(null);
        });
        block.getDesignTemplates().forEach(it -> {
            it.setDesignSettings(null);
            it.setUpdateTime(null);
        });

        block.setCreateDate(curInsertedBlock.getCreateDate());
        assertThat(curInsertedBlock).isEqualTo(block);
    }

    @Test
    void testAddOverLimit() {
        var incomingFields = new IncomingFields();
        Multimap<Model, ModelProperty<?, ?>> data
                = Multimaps.newSetMultimap(new IdentityHashMap<>(), HashSet::new);
        var modelProps = new HashSet<>(RtbBlock.allModelProperties());
        modelProps.remove(RtbBlock.MULTISTATE);
        modelProps.remove(RtbBlock.CREATE_DATE);
        modelProps.remove(RtbBlock.ID);
        var blocks = Lists.<RtbBlock>newArrayListWithCapacity(2);
        for (int i = 0; i < 2; i++) {
            var block = prepareBlock();
            setLimit(block.getPageId(), 1L);
            data.putAll(block, modelProps);

            var strategyProperties = new HashSet<>(BlockWithStrategy.allModelProperties());
            data.putAll(block, strategyProperties);

            var designTemplatesProps = new HashSet<>(CommonDesignTemplates.allModelProperties());
            designTemplatesProps.remove(CommonDesignTemplates.MULTISTATE);
            block.getDesignTemplates().forEach(it -> data.putAll(it, designTemplatesProps));
            block.getCampaign().setBlocksLimit(1L);
            blocks.add(block);
        }
        incomingFields.addIncomingFields(data);
        var countBlockBefore = blockService.count(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.neutral()));
        var container = addOperationFactory.prepareAddOperationContainer(
                OperationMode.ADD, incomingFields);
        container.getReachablePages().put(RtbBlock.class, getPages(blocks));
        container.setCanValidateDesignAsManager(blocks.stream()
                .collect(Collectors.toMap(x -> x.getClass(), x -> Boolean.TRUE, (a, b) -> a)));
        var transactionTemplate = new TransactionTemplate(ptm);
        var result = transactionTemplate.execute(it -> addOperationFactory
                .createAddOperation(Applicability.PARTIAL, blocks, container).prepareAndApply());
        var hasLimitDefect = result.getValidationResult().flattenErrors().stream().anyMatch(it ->
                it.getDefect().defectId().equals(LIMIT_EXCEEDED));
        assertThat(hasLimitDefect).isTrue();
        var countBlockAfter = blockService.count(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.neutral()));
        //пытались добавить 2 блока, но добавился только 1 из-за лимита
        assertThat(countBlockAfter - countBlockBefore).isEqualTo(1L);
    }


    @Test
    void addBlockAction() {
        var incomingFields = new IncomingFields();
        Multimap<Model, ModelProperty<?, ?>> data
                = Multimaps.newSetMultimap(new IdentityHashMap<>(), HashSet::new);
        var modelProps = new HashSet<>(RtbBlock.allModelProperties());
        modelProps.remove(RtbBlock.MULTISTATE);
        modelProps.remove(RtbBlock.CREATE_DATE);
        modelProps.remove(RtbBlock.ID);
        var blocks = Lists.<RtbBlock>newArrayListWithCapacity(2);
        for (int i = 0; i < 2; i++) {
            var block = prepareBlock();
            data.putAll(block, modelProps);

            var strategyProperties = new HashSet<>(BlockWithStrategy.allModelProperties());
            data.putAll(block, strategyProperties);

            var designTemplatesProps = new HashSet<>(CommonDesignTemplates.allModelProperties());
            designTemplatesProps.remove(CommonDesignTemplates.MULTISTATE);
            block.getDesignTemplates().forEach(it -> data.putAll(it, designTemplatesProps));

            // TODO: uncomment and check after PI-25843, right now on add pre-validation conflicts with general
            //  validation on this field
            // block2.setBlockId(199L);

            blocks.add(block);
        }
        incomingFields.addIncomingFields(data);

        var page = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                .withFilter(CoreFilterNode.eq(PageFilters.PAGE_ID, blocks.get(0).getPageId()))
        ).get(0);
        assertThat(page.getBlocksCount()).isEqualTo(0);
        var pageId = page.getId();

        OperationContainerConfigurer<BlockContainer> configurer = new OperationContainerConfigurer<BlockContainer>() {
            @Override
            public void configureContainer(BlockContainer blockContainer, Collection<BaseBlock> preloadedModels) {
                blockContainer.setPageReachabilityFilter(preloadedModels.stream().collect(
                        Collectors.toMap(x -> x.getClass(),
                                x -> CoreFilterNode.create(PageFilters.PAGE_ID, FilterOperator.EQUALS, pageId),
                                (a, b) -> a)));
                blockContainer.setCanValidateDesignAsManager(blocks.stream()
                        .collect(Collectors.toMap(x -> x.getClass(), x -> Boolean.FALSE, (a, b) -> a)));
            }

            @Override
            public Class getModelClass() {
                return BaseBlock.class;
            }
        };

        testOperationContainerConfigurer.setCurrentConfigurer(configurer);

        try {
            var actionResult = blockAddService.addModels(
                    RtbBlock.class, blocks, ContextPage.class,
                    incomingFields, OperationMode.ADD, c -> {
                    });
            assertThat(actionResult.isCommitted()).isTrue();

            for (int i = 0; i < 2; i++) {
                var newId = actionResult.getResults().get(RtbBlock.class).iterator().next()
                        .get(i).getResult().getId();

                var insertedBlock = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                        .withFilter(CoreFilterNode.eq(BlockFilters.ID, newId))
                ).get(0);
                page = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                        .withFilter(CoreFilterNode.eq(PageFilters.PAGE_ID, pageId))
                ).get(0);

                assertThat(insertedBlock.getMultistate().test(BlockStateFlag.CHECK_STATISTICS)).isFalse();
                assertThat(insertedBlock.getMultistate().test(BlockStateFlag.NEED_UPDATE)).isTrue();

                assertThat(page.getBlocksCount()).isEqualTo(2);
            }
        } finally {
            testOperationContainerConfigurer.setCurrentConfigurer(null);
        }
    }

    private RtbBlock prepareBlock() {
        var block = blockService.findAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(CoreFilterNode.eq(BlockFilters.ID, 347649081345L))
        ).get(0);
        //зануляем чтобы пройти валидацию
        block.setBlockId(null);
        block.setMultistate(null);
        block.setCreateDate(null);
        block.setId(null);
        //зануляем, чтобы протестировать установку default
        block.setCustomBkOptions(null);
        block.setHorizontalAlign(null);

        //зануляем элементы, чтобы пройти валидацию
        block.getBrands().forEach(it -> {
            it.setId(null);
            it.setBlockId(null);
            it.setPageId(null);
        });

        block.getDesignTemplates().forEach(it -> {
            it.setId(null);
            it.setBlockId(null);
            it.setPageId(null);
            it.setMultistate(null);
        });
        block.getPiCategories().forEach(it -> {
            it.setBlockId(null);
            it.setPageId(null);
        });
        return block;
    }

    private void setLimit(Long pageId, Long limit) {
        dslContext.update(PAGES)
                .set(PAGES.BLOCKS_LIMIT, limit)
                .where(PAGES.ID.eq(pageId))
                .execute();

    }

    private Map<Long, ContextPage> getPages(List<RtbBlock> blocks) {
        return pageService.findAll(QueryOpts.forClass(ContextPage.class)
                        .withFilter(CoreFilterNode.in(PageFilters.PAGE_ID,
                                blocks.stream()
                                        .map(RtbBlock::getPageId)
                                        .collect(Collectors.toList())
                        ))
                ).stream()
                .collect(Collectors.toMap(ContextPage::getId, Function.identity()));
    }

    private Map<Long, ContextPage> getPagesMobile(List<MobileRtbBlock> blocks) {
        return pageService.findAll(QueryOpts.forClass(ContextPage.class)
                        .withFilter(CoreFilterNode.in(PageFilters.PAGE_ID,
                                blocks.stream()
                                        .map(MobileRtbBlock::getPageId)
                                        .collect(Collectors.toList())
                        ))
                ).stream()
                .collect(Collectors.toMap(ContextPage::getId, Function.identity()));
    }
}
