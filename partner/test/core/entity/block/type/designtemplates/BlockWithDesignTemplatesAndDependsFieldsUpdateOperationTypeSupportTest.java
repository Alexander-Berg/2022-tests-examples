package ru.yandex.partner.core.entity.block.type.designtemplates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.type.designtemplates.designsettings.DesignSettingsAdjustService;
import ru.yandex.partner.core.entity.designtemplates.model.DesignTemplates;
import ru.yandex.partner.core.service.msf.dto.FormatSettingDto;
import ru.yandex.partner.core.service.msf.dto.FormatWithSettingsDto;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
class BlockWithDesignTemplatesAndDependsFieldsUpdateOperationTypeSupportTest {
    @Autowired
    BlockWithDesignTemplatesAndDependsFieldsUpdateOperationTypeSupport updateSupport;

    @Autowired
    DesignSettingsAdjustService designSettingsAdjustService;

    @Test
    void adjustTemplateSettings() {
        var format = new FormatWithSettingsDto();
        format.setSettings(List.of(
                setting("horizontalAlign", "boolean"),
                setting("interscroller", "boolean"),
                setting("fullscreenDuration", "integer"),
                setting("integerUnderString", "integer"),
                setting("objectNotBroken", "object"),
                setting("arrayNotBroken", "array")
        ));

        Map<String, Object> designSettings = new HashMap<>(Map.of(
                "horizontalAlign", 1,
                "interscroller", 0,
                "fullscreenDuration", 1.6,
                "integerUnderString", "10",
                "unknownProp", "10",
                "objectNotBroken", Map.of(),
                "arrayNotBroken", List.of()
        ));

        var designTemplate = new DesignTemplates()
                .withDesignSettings(designSettings);
        designSettingsAdjustService.adjustTemplateSettings(
                format,
                List.of(designTemplate)
        );

        assertThat(designTemplate.getDesignSettings()).isEqualTo(Map.of(
                "horizontalAlign", true,
                "interscroller", false,
                // round up!
                "fullscreenDuration", 2,
                "integerUnderString", 10,
                // don't touch unknown
                "unknownProp", "10",
                "objectNotBroken", Map.of(),
                "arrayNotBroken", List.of()
        ));
    }

    private FormatSettingDto setting(String name, String jsType) {
        var settingDto = new FormatSettingDto();
        settingDto.setName(name);
        settingDto.setJsType(jsType);
        return settingDto;
    }

}
