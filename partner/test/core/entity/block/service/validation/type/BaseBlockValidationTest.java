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
import ru.yandex.partner.core.entity.block.service.BlockValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.core.validation.defects.ids.TypeDefectIds.DATA_MUST_BE_INTEGER_NUMBER;

@CoreTest
public class BaseBlockValidationTest extends BaseValidationTest {
    @Autowired
    BlockValidationService validationService;

    @Autowired
    BlockTypedRepository repository;

    @Test
    void validateInvalidMultistate() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);

        block.setMultistate(null);
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        DefectInfo<Defect> defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(DATA_MUST_BE_INTEGER_NUMBER);
    }
}
