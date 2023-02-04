package ru.yandex.partner.core.configuration;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import ru.yandex.partner.core.entity.block.type.commonshowvideoandstrategy.SiteVersionType;
import ru.yandex.partner.core.service.msf.FormatSystemService;
import ru.yandex.partner.core.service.msf.dto.DefaultFormatDto;
import ru.yandex.partner.core.service.msf.dto.FormatSettingDto;
import ru.yandex.partner.core.service.msf.dto.FormatWithSettingsDto;
import ru.yandex.partner.core.service.msf.dto.MsfMessageDto;
import ru.yandex.partner.core.service.msf.dto.MsfValidationResultDto;

@TestConfiguration
public class MockFormatSystemConfiguration {
    @SuppressWarnings("checkstyle:parameternumber")
    @Bean
    public FormatSystemService formatSystemService(
            ObjectMapper objectMapper,
            @Value("classpath:mock/format/api_format_system_formats_240x400.json")
                    Resource format240x400,
            @Value("classpath:mock/format/api_format_system_formats_240x400_mobile.json")
                    Resource format240x400mobile,
            @Value("classpath:mock/format/api_format_system_formats_adaptive.json")
                    Resource formatAdaptive,
            @Value("classpath:mock/format/api_format_system_formats_poster_horizontal.json")
                    Resource formatPosterHorizontal,
            @Value("classpath:mock/format/api_format_system_formats_adaptive0418_mobile.json")
                    Resource formatAdaptive0418Mobile,
            @Value("classpath:mock/format/api_format_system_formats_adaptive0418.json")
                    Resource formatAdaptive0418,
            @Value("classpath:mock/format/api_format_system_pcode_settings.json")
                    Resource pCodeSettings
    ) {
        return new FormatSystemService() {
            @Override
            public MsfValidationResultDto validate(String lang, String userRole, String site,
                                                   Map<String, Object> data) {
                Set<String> mandatoryFields = new HashSet<>(List.of("name", "limit"));
                if ("adaptive".equals(data.get("name"))) {
                    mandatoryFields.addAll(List.of("width", "height"));
                }
                boolean hasMissedField = mandatoryFields.stream()
                        .anyMatch(f -> !data.containsKey(f));

                if (hasMissedField) {
                    var invalidResult = new MsfValidationResultDto().withValid(false);
                    var message = new MsfMessageDto();
                    message.setType("ERR");
                    message.setText("Mocked error message for missed mandatory field");
                    var mandatoryFieldsValidationResult = new MsfValidationResultDto();
                    mandatoryFieldsValidationResult.setMessages(List.of(message));
                    invalidResult.setItems(Map.of("limit", mandatoryFieldsValidationResult));
                    invalidResult.setMessages(List.of());
                    return invalidResult;
                }

                int limit = Integer.parseInt(String.valueOf(data.getOrDefault("limit", 0)));
                if (limit > 10 || (limit < 2 && data.containsKey("layout"))) {
                    var invalidResult = new MsfValidationResultDto().withValid(false);
                    var message = new MsfMessageDto();
                    message.setType("ERR");
                    message.setText("Mocked error message for limit out of bounds");
                    var limitValidationResult = new MsfValidationResultDto();
                    limitValidationResult.setMessages(List.of(message));
                    invalidResult.setItems(Map.of("limit", limitValidationResult));
                    invalidResult.setMessages(List.of());
                    return invalidResult;
                }

                if (!(data.getOrDefault("width", 0) instanceof Number)) {
                    var invalidResult = new MsfValidationResultDto().withValid(false);
                    var message = new MsfMessageDto();
                    message.setType("ERR");
                    message.setText("Type of message is not number");
                    var limitValidationResult = new MsfValidationResultDto();
                    limitValidationResult.setMessages(List.of(message));
                    invalidResult.setItems(Map.of("width", limitValidationResult));
                    invalidResult.setMessages(List.of());
                    return invalidResult;
                }

                return new MsfValidationResultDto().withValid(true);
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

                List<FormatSettingDto> pcodeSettings;
                if (SiteVersionType.MOBILE_FULLSCREEN.getLiteral().equals(siteVersion)) {
                    try {
                        pcodeSettings = objectMapper.readValue(pCodeSettings.getURL(),
                                new TypeReference<>() {
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    pcodeSettings = List.of();
                }
                return new DefaultFormatDto(limits, pcodeSettings);
            }

            @Override
            public FormatWithSettingsDto getFormatSettings(String formatName, String lang, String userRole,
                                                           String site) {
                if (formatName == null) {
                    throw new IllegalArgumentException("format name required");
                }
                try {
                    if ("adaptive".equals(formatName)) {
                        return objectMapper.readValue(formatAdaptive.getURL(),
                                FormatWithSettingsDto.class);
                    }
                    if ("240x400".equals(formatName) && "mobile".equals(site)) {
                        return objectMapper.readValue(format240x400mobile.getURL(),
                                FormatWithSettingsDto.class);
                    }
                    if ("adaptive0418".equals(formatName) && "mobile".equals(site)) {
                        return objectMapper.readValue(formatAdaptive0418Mobile.getURL(),
                                FormatWithSettingsDto.class);
                    }
                    if ("adaptive0418".equals(formatName) && "desktop".equals(site)) {
                        return objectMapper.readValue(formatAdaptive0418.getURL(),
                                FormatWithSettingsDto.class);
                    }
                    if ("posterHorizontal".equals(formatName)) {
                        return objectMapper.readValue(formatPosterHorizontal.getURL(),
                                FormatWithSettingsDto.class);
                    }
                    return objectMapper.readValue(format240x400.getURL(),
                            FormatWithSettingsDto.class);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
