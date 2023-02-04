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
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.Geo;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.service.BaseValidationTest;
import ru.yandex.partner.core.validation.defects.ids.PartnerCollectionDefectIds;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_GREATER_THAN_MIN;
import static ru.yandex.partner.core.CoreConstants.MAX_CPM;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.Strategy.INCORRECT_MINCPM_VALUE;

@CoreTest
public class BlockWithGeoValidationTest extends BaseValidationTest {

    @Autowired
    BlockTypedRepository repository;

    @Test
    void validateCorrectGeoList() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        assertThat(block.getGeo()).size().isEqualTo(1);

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(0);
    }

    @Test
    void validateGeoEmptyList() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        block.setGeo(Collections.emptyList());

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(0);
    }

    @Test
    void validateGeoNull() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        block.setGeo(null);

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(0);
    }

    @Test
    void validateGeoAllNulls() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        var geo = new Geo();

        block.setGeo(List.of(geo));

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(2);

        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(DefectIds.CANNOT_BE_NULL);
        assertThat(defectInfos.get(0).getPath().toString()).isEqualTo("[0].geo[0].cpm");

        assertThat(defectInfos.get(1).getDefect().defectId()).isEqualTo(DefectIds.CANNOT_BE_NULL);
        assertThat(defectInfos.get(1).getPath().toString()).isEqualTo("[0].geo[0].id");

    }

    @Test
    void validateGeoRanges() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        List<Geo> geo = List.of(
                new Geo().withId(-1L).withCpm(BigDecimal.ZERO),
                new Geo().withId(0L).withCpm(MAX_CPM.add(BigDecimal.ONE))
        );

        block.setGeo(geo);

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(4);

        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(INCORRECT_MINCPM_VALUE);
        assertThat(defectInfos.get(0).getPath().toString()).isEqualTo("[0].geo[0].cpm");

        assertThat(defectInfos.get(1).getDefect().defectId()).isEqualTo(MUST_BE_GREATER_THAN_MIN);
        assertThat(defectInfos.get(1).getPath().toString()).isEqualTo("[0].geo[0].id");

        assertThat(defectInfos.get(2).getDefect().defectId()).isEqualTo(INCORRECT_MINCPM_VALUE);
        assertThat(defectInfos.get(2).getPath().toString()).isEqualTo("[0].geo[1].cpm");

        assertThat(defectInfos.get(3).getDefect().defectId()).isEqualTo(MUST_BE_GREATER_THAN_MIN);
        assertThat(defectInfos.get(3).getPath().toString()).isEqualTo("[0].geo[1].id");

    }

    @Test
    void validateDuplicateIds() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        List<Geo> geo = List.of(
                new Geo().withId(1L).withCpm(BigDecimal.valueOf(15L)),
                new Geo().withId(2L).withCpm(BigDecimal.valueOf(35L)),
                new Geo().withId(1L).withCpm(BigDecimal.valueOf(10L)),
                new Geo().withId(2L).withCpm(BigDecimal.valueOf(25L))
        );

        block.setGeo(geo);

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(1);

        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(
                PartnerCollectionDefectIds.Entries.DUPLICATE_ENTRIES);
        assertThat(defectInfos.get(0).getPath().toString()).isEqualTo("[0].geo");
        assertThat(defectInfos.get(0).getDefect().params()).isEqualTo(Set.of(1L, 2L));

    }

    @Test
    void validateNonExistsIds() {
        RtbBlock block = (RtbBlock) repository.getBlockByPageIdAndBlockId(41443L, 1L);

        List<Geo> geo = List.of(
                //exists
                new Geo().withId(1L).withCpm(BigDecimal.valueOf(15L)),
                //not exists
                new Geo().withId(10500L).withCpm(BigDecimal.valueOf(25L)),
                new Geo().withId(100500L).withCpm(BigDecimal.valueOf(35L))
        );

        block.setGeo(geo);

        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();

        assertThat(defectInfos).size().isEqualTo(1);

        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(
                PartnerCollectionDefectIds.Entries.ENTRIES_NOT_FOUND);
        assertThat(defectInfos.get(0).getPath().toString()).isEqualTo("[0].geo");
        assertThat(defectInfos.get(0).getDefect().params()).isEqualTo(Set.of(10500L, 100500L));

    }

}
