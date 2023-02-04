package ru.yandex.partner.core.entity.block.service.validation.type;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.service.BaseValidationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL;

@CoreTest
class BlockWithAlternativeCodeValidationTest extends BaseValidationTest {

    @Autowired
    BlockTypedRepository repository;

    @Test
    void validateIncorrectAlternativeCode() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);
        block.setAlternativeCode(null);
        List<DefectInfo<Defect>> defectInfos = validate(List.of(block)).flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(CANNOT_BE_NULL);
    }
}
