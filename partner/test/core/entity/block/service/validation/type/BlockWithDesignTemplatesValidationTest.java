package ru.yandex.partner.core.entity.block.service.validation.type;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.BlockWithDesignTemplatesAndDependsFields;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.service.BaseValidationTest;
import ru.yandex.partner.core.entity.block.service.OperationMode;
import ru.yandex.partner.core.entity.designtemplates.model.DesignTemplates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.direct.validation.defect.ids.StringDefectIds.LENGTH_CANNOT_BE_LESS_THAN_MIN;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.DesignTemplates.INVALID_SET_OF_NATIVE_DESIGN;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.DesignTemplates.MSF_FAILED;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.DesignTemplates.TOO_MANY_DESIGNS;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.DesignTemplates.TYPE_FIELD_CHANGES;
import static ru.yandex.partner.dbschema.partner.enums.DesignTemplatesType.native_;
import static ru.yandex.partner.dbschema.partner.enums.DesignTemplatesType.tga;
import static ru.yandex.partner.dbschema.partner.enums.DesignTemplatesType.video;

@CoreTest
class BlockWithDesignTemplatesValidationTest extends BaseValidationTest {
    @Autowired
    BlockTypedRepository repository;

    BlockWithDesignTemplatesValidationTest() {
        super(OperationMode.EDIT);
    }

    @Test
    void validateIncorrectDesignTemplates() {
        var block = (BlockWithDesignTemplatesAndDependsFields) repository.getBlockByCompositeId(347649081345L);

        var designTemplate = block.getDesignTemplates().get(0);
        block.setSiteVersion("amp");
        designTemplate.setType(native_);
        var unchangedTemplates = ((BlockWithDesignTemplatesAndDependsFields) repository
                .getBlockByCompositeId(347649081345L)).getDesignTemplates();
        getContainer().setUnmodifiedTemplates(Map.of(347649081345L, unchangedTemplates));
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos).size().isEqualTo(1);
        DefectInfo<Defect> defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(INVALID_SET_OF_NATIVE_DESIGN);

        block.setSiteVersion("desktop");
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertFalse(defectInfos.isEmpty());
        assertTrue(defectInfos.stream().anyMatch(it -> it.getDefect().defectId().equals(TYPE_FIELD_CHANGES)));

        designTemplate.setType(video);
        designTemplate.setCaption("");
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos).size().isEqualTo(1);
        defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(TOO_MANY_DESIGNS);

        designTemplate.setType(tga);
        designTemplate.setCaption("");
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos).size().isEqualTo(1);
        defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(LENGTH_CANNOT_BE_LESS_THAN_MIN);
    }

    @Test
    void validateMSFLayoutAndLimitIncorrectDesignTemplates() {
        var block = (BlockWithDesignTemplatesAndDependsFields) repository.getBlockByCompositeId(347649081345L);

        var designTemplate = block.getDesignTemplates().get(0);
        block.setSiteVersion("desktop");
        designTemplate.setType(tga);

        Map<String, Object> unModifiedDesignSettings = Map.of(
                "name", "adaptive0418",
                "limit", 2,
                "layout", "horizontal"
        );
        Map<String, Object> designSettings = Map.of(
                "name", "adaptive0418",
                "limit", 2,
                "layout", "vertical"
        );
        designTemplate.setDesignSettings(designSettings);
        var unchangedTemplates = ((BlockWithDesignTemplatesAndDependsFields) repository
                .getBlockByCompositeId(347649081345L)).getDesignTemplates();
        DesignTemplates unchangedDesignTemplates = unchangedTemplates.get(0);
        unchangedDesignTemplates.setType(tga);
        unchangedDesignTemplates.setDesignSettings(unModifiedDesignSettings);
        getContainer().setUnmodifiedTemplates(Map.of(347649081345L, unchangedTemplates));
        ValidationResult<List<? extends BaseBlock>, Defect> vr = validate(List.of(block));
        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertTrue(defectInfos.isEmpty());


        designSettings = Map.of(
                "name", "adaptive0418",
                "limit", 1,
                "layout", "vertical"
        );
        designTemplate.setDesignSettings(designSettings);
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(1);
        DefectInfo<Defect> defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(MSF_FAILED);

        designSettings = Map.of(
                "name", "adaptive0418",
                "limit", 1
        );
        designTemplate.setDesignSettings(designSettings);
        vr = validate(List.of(block));
        defectInfos = vr.flattenErrors();
        assertTrue(defectInfos.isEmpty());
    }

}

