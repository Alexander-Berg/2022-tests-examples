package ru.yandex.partner.core.entity.block.type.designtemplates;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.model.BlockWithDesignTemplatesAndDependsFields;
import ru.yandex.partner.core.entity.block.repository.BlockTypedRepository;
import ru.yandex.partner.core.entity.block.service.OperationMode;
import ru.yandex.partner.core.entity.block.type.commonshowvideoandstrategy.SiteVersionType;
import ru.yandex.partner.core.entity.block.type.designtemplates.designsettings.DesignSettingsValidatorHelper;
import ru.yandex.partner.core.service.msf.FormatSystemService;
import ru.yandex.partner.core.service.msf.dto.DefaultFormatDto;
import ru.yandex.partner.core.service.msf.dto.FormatGroupDto;
import ru.yandex.partner.core.service.msf.dto.FormatWithSettingsDto;
import ru.yandex.partner.core.service.msf.dto.MsfMessageDto;
import ru.yandex.partner.core.service.msf.dto.MsfValidationResultDto;
import ru.yandex.partner.dbschema.partner.enums.DesignTemplatesType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.DesignTemplates.INVALID_DESIGN_TYPE;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.DesignTemplates.MSF_FAILED;
import static ru.yandex.partner.core.entity.block.service.validation.defects.BlockDefectIds.DesignTemplates.NOT_AVAILABLE_DESIGN_SETTING;

@CoreTest
class BlockWithDesignTemplatesSiteVersionCommonShowVideoValidatorProviderTest {
    @Autowired
    BlockTypedRepository repository;

    @Test
    void testTgaDesignByPartner() {
        var block = (BlockWithDesignTemplatesAndDependsFields)
                repository.getBlockByCompositeId(347649081345L);

        var designTemplate = block.getDesignTemplates().get(0);
        DesignTemplatesContainer container = new DesignTemplatesContainer()
                .withValidateAsManager(false)
                .withValidationMode(OperationMode.EDIT)
                .withSiteVersion("dekstop")
                .withAvailableDesignTypes(Set.of());
        var provider = new DesignTemplatesValidatorProvider(new FormatSystemService() {
            @Override
            public List<FormatGroupDto> getFormats(String form, String lang, String userRole, String site) {
                return null;
            }

            @Override
            public FormatWithSettingsDto getFormatSettings(String formatName, String lang, String userRole,
                                                           String site) {
                var formatSettings = new FormatWithSettingsDto();
                formatSettings.setSettings(List.of());
                return formatSettings;
            }

            @Override
            public MsfValidationResultDto validate(String lang, String userRole, String site, Map<String,
                    Object> data) {
                var result = new MsfValidationResultDto().withValid(false);
                var message = new MsfMessageDto();
                message.setText("msf failed");
                result.setMessages(List.of(message));
                result.setItems(Map.of());
                return result;
            }

            @Override
            public MsfValidationResultDto validatePCodeSettings(String lang, String userRole, String site,
                                                                Boolean isAdfox, Map<String, Object> data) {
                return new MsfValidationResultDto().withValid(true);
            }

            @Override
            public DefaultFormatDto getDefaultFormats(String lang, String userRole,
                                                      String siteVersion, Boolean isAdfox) {
                Map<String, Integer> limits;
                if (siteVersion.equals(SiteVersionType.MOBILE_FULLSCREEN.getLiteral())) {
                    limits = Map.of("media", 1,
                            "tga", 1,
                            "video", 1,
                            "native", 0);
                } else {
                    limits = Map.of("media", 1,
                            "tga", 20,
                            "video", 1,
                            "native", 20);
                }
                return new DefaultFormatDto(limits, List.of());
            }
        }, new DesignSettingsValidatorHelper());
        var result = provider.validator(container).apply(designTemplate);
        var defectInfos = result.flattenErrors();
        assertThat(defectInfos).size().isEqualTo(1);
        var defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(INVALID_DESIGN_TYPE);

        container.setAvailableDesignTypes(Set.of(DesignTemplatesType.tga));
        result = provider.validator(container).apply(designTemplate);
        defectInfos = result.flattenErrors();
        assertFalse(defectInfos.isEmpty());
        defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(NOT_AVAILABLE_DESIGN_SETTING);

        container.setValidateAsManager(true);
        result = provider.validator(container).apply(designTemplate);
        defectInfos = result.flattenErrors();
        assertFalse(defectInfos.isEmpty());
        defect = defectInfos.get(0);
        assertThat(defect.getDefect().defectId()).isEqualTo(MSF_FAILED);
    }


}
