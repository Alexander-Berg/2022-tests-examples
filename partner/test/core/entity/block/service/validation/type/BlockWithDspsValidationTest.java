package ru.yandex.partner.core.entity.block.service.validation.type;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.container.BlockContainerImpl;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.service.BlockValidationService;
import ru.yandex.partner.core.entity.block.service.OperationMode;
import ru.yandex.partner.core.entity.dsp.model.Dsp;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL;
import static ru.yandex.partner.core.validation.defects.ids.TypeDefectIds.DATA_MUST_BE_DEFINED;

@CoreTest
class BlockWithDspsValidationTest {
    @Autowired
    BlockValidationService validationService;

    @Autowired
    BlockTypedRepository repository;

    @Test
    void validateIncorrectNullBlockDsps() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);
        block.setDsps(null);
        List<DefectInfo<Defect>> defectInfos = validationService.validate(List.of(block),
                BlockContainerImpl.create(OperationMode.CRON)).flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(DATA_MUST_BE_DEFINED);
    }

    @Test
    void validateIncorrectUnmoderatedRtbAuction() {
        RtbBlock block = (RtbBlock) repository.getBlockByCompositeId(347649081345L);

        for (Dsp blockDsp : block.getDsps()) {
            blockDsp.setUnmoderatedRtbAuction(null);
        }

        List<DefectInfo<Defect>> defectInfos = validationService.validate(List.of(block),
                BlockContainerImpl.create(OperationMode.CRON)).flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(CANNOT_BE_NULL);
    }
}
