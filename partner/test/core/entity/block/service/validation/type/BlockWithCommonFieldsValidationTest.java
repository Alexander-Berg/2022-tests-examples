package ru.yandex.partner.core.entity.block.service.validation.type;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import static ru.yandex.direct.validation.defect.ids.StringDefectIds.LENGTH_CANNOT_BE_LESS_THAN_MIN;
import static ru.yandex.direct.validation.defect.ids.StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX;
import static ru.yandex.partner.core.validation.defects.ids.TypeDefectIds.DATA_MUST_BE_DEFINED;

@CoreTest
public class BlockWithCommonFieldsValidationTest extends BaseValidationTest {
    @Autowired
    BlockTypedRepository repository;

    @Test
    void validateIncorrectCaption() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);

        block.setCaption(null);
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        DefectInfo<Defect> defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(DATA_MUST_BE_DEFINED);

        block.setCaption("");
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(LENGTH_CANNOT_BE_LESS_THAN_MIN);

        block.setCaption(StringUtils.repeat("*", 256));
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(LENGTH_CANNOT_BE_MORE_THAN_MAX);

        block.setCaption(StringUtils.repeat("*", 255));
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos).isEmpty();
    }
}
