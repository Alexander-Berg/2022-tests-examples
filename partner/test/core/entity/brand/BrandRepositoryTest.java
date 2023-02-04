package ru.yandex.partner.core.entity.brand;

import java.util.List;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.PageBlockIds;
import ru.yandex.partner.core.entity.block.model.Brand;
import ru.yandex.partner.core.entity.brand.filter.BrandFilters;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.filter.operator.BinaryOperator;
import ru.yandex.partner.core.junit.MySqlRefresher;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MySqlRefresher.class)
@CoreTest
public class BrandRepositoryTest {
    @Autowired
    DSLContext dslContext;
    @Autowired
    BrandRepository brandRepository;

    @Test
    void insertBrand() {
        brandRepository.insert(List.of(
                new Brand()
                        .withPageId(41443L)
                        .withBlockId(1L)
                        .withBlocked(false)
                        .withBid(52L)
        ));

        PageBlockIds pageBlockId = new PageBlockIds(41443L, 1L);

        var filter = new CoreFilterNode(BinaryOperator.AND, List.of(
                CoreFilterNode.eq(
                        BrandFilters.PAGE_ID,
                        pageBlockId.getPageId()),
                CoreFilterNode.eq(
                        BrandFilters.BLOCK_ID,
                        pageBlockId.getBlockId()))
        );

        assertThat(brandRepository.getAll(filter, null, null, false))
                .hasSize(2);
    }

    @Test
    void createOrUpdateBrand() {


        PageBlockIds pageBlockId = new PageBlockIds(41443L, 1L);

        var filter = new CoreFilterNode(BinaryOperator.AND, List.of(
                CoreFilterNode.eq(
                        BrandFilters.PAGE_ID,
                        pageBlockId.getPageId()),
                CoreFilterNode.eq(
                        BrandFilters.BLOCK_ID,
                        pageBlockId.getBlockId()))
        );

        var brandToUpdate = (Brand) brandRepository
                .getAll(filter, null, null, false).get(0);

        brandToUpdate.setBlocked(!brandToUpdate.getBlocked());

        var result  = brandRepository.createOrUpdate(List.of(brandToUpdate));

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0))
                .isEqualToComparingOnlyGivenFields(brandToUpdate, "blocked");

    }

    @Test
    void deleteBrand() {
        PageBlockIds pageBlockId = new PageBlockIds(41443L, 1L);

        var filter = new CoreFilterNode(BinaryOperator.AND, List.of(
                CoreFilterNode.eq(
                        BrandFilters.PAGE_ID,
                        pageBlockId.getPageId()),
                CoreFilterNode.eq(
                        BrandFilters.BLOCK_ID,
                        pageBlockId.getBlockId()))
        );

        var existingBrands = brandRepository.getAll(filter, null, null, false);

        brandRepository.delete(existingBrands);

        assertThat(brandRepository.getAll(filter, null, null, false)).isEmpty();
    }
}
