package ru.yandex.partner.core.entity.block.service.validation.type;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.exception.DefectInfoWithMsgParams;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.Brand;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.service.BaseValidationTest;
import ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.BrandDefectIds;
import ru.yandex.partner.core.validation.defects.ids.PartnerCollectionDefectIds;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.Strategy.INCORRECT_MINCPM_VALUE;
import static ru.yandex.partner.core.validation.defects.presentation.CommonValidationMsg.MUST_BE_UINT;

@CoreTest
public class BlockWithBrandsValidationTest extends BaseValidationTest {

    @Autowired
    BlockTypedRepository repository;

    @Test
    void validateCorrectBrands() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        assertThat(block.getBrands()).size().isEqualTo(1);

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(0);
    }

    @Test
    void validateBrandsNull() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        block.setBrands(null);

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(0);
    }

    @Test
    void validateBrandsEmptyList() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        block.setBrands(Collections.emptyList());

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(0);
    }

    @Test
    void validateBrandAllNulls() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        assertThat(block.getBrands()).size().isEqualTo(1);

        block.getBrands().get(0).setBid(null);
        block.getBrands().get(0).setBlocked(null);
        block.getBrands().get(0).setCpm(null);

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(2);

        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(DefectIds.CANNOT_BE_NULL);
        assertThat(defectInfos.get(0).getPath().toString()).isEqualTo("[0].brands[0].blocked");

        assertThat(defectInfos.get(1).getDefect().defectId()).isEqualTo(DefectIds.CANNOT_BE_NULL);
        assertThat(defectInfos.get(1).getPath().toString()).isEqualTo("[0].brands[0].bid");

    }

    @Test
    void validateBrandRanges() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        assertThat(block.getBrands()).size().isEqualTo(1);

        block.getBrands().get(0).withBid(-1L).withCpm(BigDecimal.ZERO);

        block.getBrands().add(new Brand().withBid(-1L).withCpm(BigDecimal.valueOf(10000L)).withBlocked(false));

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(4);

        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(INCORRECT_MINCPM_VALUE);
        assertThat(defectInfos.get(0).getPath().toString()).isEqualTo("[0].brands[0].cpm");

        assertThat(((DefectInfoWithMsgParams) defectInfos.get(1).getDefect().params()).getGettextMsg())
                .isEqualTo(MUST_BE_UINT);
        assertThat(defectInfos.get(1).getPath().toString()).isEqualTo("[0].brands[0].bid");

        assertThat(defectInfos.get(2).getDefect().defectId()).isEqualTo(INCORRECT_MINCPM_VALUE);
        assertThat(defectInfos.get(2).getPath().toString()).isEqualTo("[0].brands[1].cpm");

        assertThat(((DefectInfoWithMsgParams) defectInfos.get(3).getDefect().params()).getGettextMsg())
                .isEqualTo(MUST_BE_UINT);
        assertThat(defectInfos.get(3).getPath().toString()).isEqualTo("[0].brands[1].bid");

    }

    @Test
    void validateInvalidState() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        assertThat(block.getBrands()).size().isEqualTo(1);

        block.getBrands().get(0).withBid(1L).withCpm(null).withBlocked(false);

        block.getBrands().add(new Brand().withBid(2L).withCpm(BigDecimal.valueOf(100L)).withBlocked(true));

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(2);

        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(BrandDefectIds.CPM_NOT_SET_FOR_NON_BLOCKED);
        assertThat(defectInfos.get(0).getPath().toString()).isEqualTo("[0].brands[0]");

        assertThat(defectInfos.get(1).getDefect().defectId()).isEqualTo(BrandDefectIds.CPM_SET_FOR_BLOCKED);
        assertThat(defectInfos.get(1).getPath().toString()).isEqualTo("[0].brands[1]");

    }


    @Test
    void validateDuplicateBids() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        assertThat(block.getBrands()).size().isEqualTo(1);

        block.getBrands().addAll(
                List.of(
                        new Brand().withBid(427L).withCpm(BigDecimal.valueOf(100L)).withBlocked(false),
                        new Brand().withBid(427L).withCpm(null).withBlocked(true),
                        new Brand().withBid(6649L).withCpm(BigDecimal.valueOf(100L)).withBlocked(false),
                        new Brand().withBid(6649L).withCpm(null).withBlocked(true)
                )
        );

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(1);

        assertThat(defectInfos.get(0).getDefect().defectId())
                .isEqualTo(PartnerCollectionDefectIds.Entries.DUPLICATE_ENTRIES);
        assertThat(defectInfos.get(0).getPath().toString()).isEqualTo("[0].brands");
        assertThat(defectInfos.get(0).getDefect().params()).isEqualTo(Set.of(6649L, 427L));
    }

    @Test
    void validateNonExistantBids() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        assertThat(block.getBrands()).size().isEqualTo(1);

        block.getBrands().addAll(
                List.of(
                        new Brand().withBid(427L).withCpm(BigDecimal.valueOf(100L)).withBlocked(false),
                        new Brand().withBid(10427L).withCpm(null).withBlocked(true),
                        new Brand().withBid(16649L).withCpm(BigDecimal.valueOf(100L)).withBlocked(false),
                        new Brand().withBid(6649L).withCpm(null).withBlocked(true)
                )
        );

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(1);

        assertThat(defectInfos.get(0).getDefect().defectId())
                .isEqualTo(PartnerCollectionDefectIds.Entries.ENTRIES_NOT_FOUND);
        assertThat(defectInfos.get(0).getPath().toString()).isEqualTo("[0].brands");
        assertThat(defectInfos.get(0).getDefect().params()).isEqualTo(Set.of(10427L, 16649L));
    }
}
