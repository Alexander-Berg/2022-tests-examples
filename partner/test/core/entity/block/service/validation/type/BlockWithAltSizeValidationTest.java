package ru.yandex.partner.core.entity.block.service.validation.type;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.service.BaseValidationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.AltSize.EMPTY_HEIGHT;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.AltSize.EMPTY_WIDTH;
import static ru.yandex.partner.core.validation.defects.ids.TypeDefectIds.DATA_MUST_BE_INTEGER_NUMBER;

@CoreTest
public class BlockWithAltSizeValidationTest extends BaseValidationTest {
    @Autowired
    BlockTypedRepository repository;

    @Test
    void validateCorrectAltSize() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);

        block.setAltWidth(null);
        block.setAltHeight(null);
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos).isEmpty();

        block.setAltWidth(1L);
        block.setAltHeight(1L);
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos).isEmpty();
    }

    @Test
    void validateIncorrectAltSize() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);

        block.setAltWidth(1L);
        block.setAltHeight(null);
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        DefectInfo<Defect> defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(EMPTY_HEIGHT);

        block.setAltWidth(null);
        block.setAltHeight(1L);
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(EMPTY_WIDTH);

        block.setAltWidth(-1L);
        block.setAltHeight(-1L);
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(2);
    }

    @Test
    void validateIncorrectAltWidth() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);

        block.setAltWidth(0L);
        block.setAltHeight(1L);
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        DefectInfo<Defect> defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(DATA_MUST_BE_INTEGER_NUMBER);
    }

    @Test
    void validateIncorrectAltHeight() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);

        block.setAltWidth(1L);
        block.setAltHeight(0L);
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        DefectInfo<Defect> defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(DATA_MUST_BE_INTEGER_NUMBER);

    }
}
