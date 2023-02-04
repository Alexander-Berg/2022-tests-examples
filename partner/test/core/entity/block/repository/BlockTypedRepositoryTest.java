package ru.yandex.partner.core.entity.block.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.multitype.repository.filter.ConditionFilterFactory;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.block.BlockUniqueIdConverter;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.BlockWithCommonFields;
import ru.yandex.partner.core.entity.block.model.BlockWithReadonly;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.type.base.BaseBlockConstants;
import ru.yandex.partner.core.entity.common.editablefields.EditableFieldsService;
import ru.yandex.partner.core.entity.page.filter.PageFilters;
import ru.yandex.partner.core.entity.page.model.BasePage;
import ru.yandex.partner.core.entity.page.model.PageWithCommonFields;
import ru.yandex.partner.core.entity.page.repository.PageTypedRepository;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.filter.dbmeta.models.PublicIdFilterModel;
import ru.yandex.partner.core.filter.operator.FilterOperator;
import ru.yandex.partner.core.junit.MySqlRefresher;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static ru.yandex.partner.dbschema.partner.Tables.PAGES;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class BlockTypedRepositoryTest {

    @Autowired
    BlockTypedRepository blockTypedRepository;
    @Autowired
    PageTypedRepository pageTypedRepository;
    @Autowired
    EditableFieldsService<BaseBlock> editableFieldsService;


    @Test
    void getBlockbyCompositeId() {
        BaseBlock block = blockTypedRepository.getBlockByCompositeId(347649081345L);
        assertThat(block).isNotNull();
        assertThat(block.getId()).isEqualTo(347649081345L);
        assertThat(block).isExactlyInstanceOf(RtbBlock.class);

        RtbBlock rtbBlock = (RtbBlock) block;
        assertThat(rtbBlock.getPageId()).isEqualTo(41443L);
        assertThat(rtbBlock.getBlockId()).isEqualTo(1L);
        assertThat(rtbBlock.getCaption()).isEqualToIgnoringCase("В списке1");
        assertThat(rtbBlock.getHorizontalAlign()).isFalse();
    }

    @Test
    void getBlocksByFilterTest() {
        CoreFilterNode<BlockWithCommonFields> node = CoreFilterNode.create(
                BlockFilters.CAPTION,
                FilterOperator.LIKE, List.of("списк"));
        List<BaseBlock> blocks = blockTypedRepository.getAll(QueryOpts.forClass(BlockWithCommonFields.class)
                .withFilter(node)
        );
        assertThat(blocks.size()).isPositive();
        blocks.forEach(block -> assertThat(((BlockWithCommonFields) block).getCaption()).contains("списк"));
    }

    @Test
    void getBlocksByMatchFilterTest() {
        CoreFilterNode<BasePage> pageNode = CoreFilterNode.create(
                PageFilters.CAPTION,
                FilterOperator.LIKE, List.of("TEST"));

        CoreFilterNode<BaseBlock> node = CoreFilterNode.create(BlockFilters.PAGE, FilterOperator.MATCH, pageNode);

        List<BaseBlock> blocks = blockTypedRepository.getAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(node)
        );
        assertThat(blocks.size()).isPositive();

        Set<Long> pageIds = blocks.stream()
                .map(BaseBlock::getPageId)
                .collect(Collectors.toSet());

        List<BasePage> pages = pageTypedRepository.getTypedModelByIds(
                ConditionFilterFactory.whereInFilter(PAGES.ID, pageIds)
        );

        assertThat(pages.size()).isPositive();
        pages.forEach(p -> assertThat(((PageWithCommonFields) p).getCaption()).containsIgnoringCase("TEST"));
    }

    @Test
    void getBlocksByNotMatchFilterTest() {
        CoreFilterNode<BasePage> pageNode = CoreFilterNode.create(
                PageFilters.CAPTION,
                FilterOperator.LIKE, List.of("TEST"));

        CoreFilterNode<? super RtbBlock> node = CoreFilterNode.create(
                BlockFilters.PAGE,
                FilterOperator.NOT_MATCH, pageNode);

        List<BaseBlock> blocks = blockTypedRepository.getAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(node)
        );
        assertThat(blocks.size()).isPositive();

        Set<Long> pageIds = blocks.stream()
                .map(BaseBlock::getPageId)
                .collect(Collectors.toSet());

        List<BasePage> pages = pageTypedRepository.getTypedModelByIds(
                ConditionFilterFactory.whereInFilter(PAGES.ID, pageIds)
        );

        assertThat(pages.size()).isPositive();
        pages.forEach(p -> assertThat(((PageWithCommonFields) p).getCaption()).doesNotContain("TEST"));
    }

    @Test
    void getBlocksByAliasFilterTest() {
        CoreFilterNode<BaseBlock> blockNode = CoreFilterNode.create(
                BlockFilters.IS_YANDEX_BLOCK,
                FilterOperator.EQUALS, true);

        List<BaseBlock> blocks = blockTypedRepository.getAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(blockNode)
        );
        assertThat(blocks.size()).isZero();

        blockNode = CoreFilterNode.create(
                BlockFilters.IS_YANDEX_BLOCK,
                FilterOperator.EQUALS, false);

        blocks = blockTypedRepository.getAll(QueryOpts.forClass(RtbBlock.class)
                .withFilter(blockNode)
        );
        assertThat(blocks.size()).isPositive();
    }

    @Test
    void getBlocksFieldsTest() {
        CoreFilterNode<BaseBlock> blockNode = CoreFilterNode.create(
                BlockFilters.PUBLIC_ID,
                FilterOperator.EQUALS,
                new PublicIdFilterModel(BlockUniqueIdConverter.Prefixes.CONTEXT_ON_SITE_RTB_PREFIX, "R-A-41446-5"));

        List<BaseBlock> blocks = blockTypedRepository.getAll(QueryOpts.forClass(BaseBlock.class)
                .withFilter(blockNode)
                .withProps(Set.of(BlockWithReadonly.READONLY))
        );


        Assertions.assertEquals(1, blocks.size());
        Assertions.assertTrue(blocks.get(0) instanceof BlockWithReadonly);
        Assertions.assertEquals(true, ((BlockWithReadonly) blocks.get(0)).getReadonly());
    }

    @Test
    void getEditableFields() {
        List<RtbBlock> blocks = blockTypedRepository.getSafely(List.of(347649081345L), RtbBlock.class);
        assertThat(editableFieldsService.calculateEditableModelPropertiesHolder(blocks.get(0), null)
                .containsAnyPath(new HashSet<>(BaseBlockConstants.EDIT_FORBIDDEN_MODEL_PROPERTIES))
        ).isFalse();

    }
}

