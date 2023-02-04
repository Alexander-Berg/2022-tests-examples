package ru.yandex.partner.core.entity.block.type.designtemplates.designsettings;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.entity.block.service.OperationMode;

import static org.assertj.core.api.Assertions.assertThat;

class TgaDesignSettingsValidatorTest {

    @Test
    void diff() {
        assertThat(TgaDesignSettingsValidator.diff(OperationMode.EDIT,
                Map.of(
                        "name", "vertical",
                        "bannerBorder", true,
                        // only bgcolor changed
                        "bgColor", "EEEEEE",
                        "borderRadius", false
                ),
                Map.of(
                        "name", "vertical",
                        "bannerBorder", true,
                        "bgColor", "FFFFFF",
                        "borderRadius", false
                )
        )).containsOnlyKeys("name", "bgColor");
    }
}
