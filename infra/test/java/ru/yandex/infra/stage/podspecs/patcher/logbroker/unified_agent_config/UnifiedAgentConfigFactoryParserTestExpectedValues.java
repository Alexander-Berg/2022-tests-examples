package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigParserTestExpectedValues.EXPECTED_DATA_RETENTION_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ErrorBoosterGenerationConfigParserTestExpectedValues.EXPECTED_ERROR_BOOSTER_GENERATION_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigParserTestExpectedValues.EXPECTED_THROTTLING_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.VersionCalculatorParserTestExpectedValues.EXPECTED_VERSION_CALCULATOR;

public class UnifiedAgentConfigFactoryParserTestExpectedValues {

    public static final UnifiedAgentConfigFactory EXPECTED_UNIFIED_AGENT_CONFIG_FACTORY = new UnifiedAgentConfigFactory(
            EXPECTED_THROTTLING_CONFIG,
            EXPECTED_ERROR_BOOSTER_GENERATION_CONFIG,
            EXPECTED_VERSION_CALCULATOR,
            EXPECTED_DATA_RETENTION_CONFIG
    );
}
