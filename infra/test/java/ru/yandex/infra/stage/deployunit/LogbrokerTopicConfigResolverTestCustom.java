package ru.yandex.infra.stage.deployunit;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.dto.LogbrokerCustomTopicRequest;
import ru.yandex.infra.stage.dto.LogbrokerTopicConfig;
import ru.yandex.infra.stage.dto.LogbrokerTopicDescription;
import ru.yandex.infra.stage.dto.LogbrokerTopicRequest;
import ru.yandex.infra.stage.dto.SecretSelector;

public class LogbrokerTopicConfigResolverTestCustom extends LogbrokerTopicConfigResolverTestBase {

    private static final String CUSTOM_PREFIX = "custom-";

    private static String toCustom(String communalString) {
        return CUSTOM_PREFIX + communalString;
    }

    public static final LogbrokerCustomTopicRequest DEFAULT_CUSTOM_TOPIC_REQUEST;
    private static final LogbrokerTopicConfig EXPECTED_CUSTOM_TOPIC_CONFIG;

    static {
        var communalTopicDescription = COMMUNAL_TOPIC_CONFIG.getTopicDescription();
        var customTopicDescription = new LogbrokerTopicDescription(
                communalTopicDescription.getTvmClientId() + 1,
                toCustom(communalTopicDescription.getName())
        );

        var customTopicSecretSelector = new SecretSelector(
                "user_specified_secret_alias",
                "user_specified_secret_key"
        );

        DEFAULT_CUSTOM_TOPIC_REQUEST = new LogbrokerCustomTopicRequest(
                customTopicDescription,
                customTopicSecretSelector
        );

        EXPECTED_CUSTOM_TOPIC_CONFIG = new LogbrokerTopicConfig(
                customTopicDescription,
                customTopicSecretSelector
        );
    }

    @Override
    LogbrokerTopicRequest getSpecTopicRequest() {
        return DEFAULT_CUSTOM_TOPIC_REQUEST;
    }

    @Test
    void getTopicConfigTest() {
        getTopicConfigTestScenario(EXPECTED_CUSTOM_TOPIC_CONFIG);
    }

}
