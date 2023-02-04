package ru.yandex.partner.core.entity.block.service.validation.type;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.block.MobileBlockType;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.MobileRtbBlock;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.service.BaseValidationTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
public class BlockWithMobileRewardValidationTest extends BaseValidationTest {
    @Autowired
    BlockTypedRepository repository;

    @Test
    void validateCorrectMobileReward() {
        MobileRtbBlock block = (MobileRtbBlock) repository.getBlockByCompositeId(72058885715787778L);

        block.setBlockType(MobileBlockType.REWARDED.getLiteral());
        block.setCurrencyType("Currency");
        block.setSign("Sign");
        block.setCallback("Callback");
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos).isEmpty();

        block.setCurrencyType("Currency");
        block.setSign(null);
        block.setCallback(null);
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos).isEmpty();
    }

    @Test
    void validateIncorrectMobileReward() {
        MobileRtbBlock block = (MobileRtbBlock) repository.getBlockByCompositeId(72058885715787778L);

        block.setBlockType(MobileBlockType.REWARDED.getLiteral());
        block.setCurrencyType("");
        block.setSign("");
        block.setCallback("");
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(3);

        block.setCurrencyType(new Random().ints(97, 123).limit(65)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
        block.setSign(new Random().ints(97, 123).limit(65)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
        block.setCallback(new Random().ints(97, 123).limit(256)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(3);


    }
}
