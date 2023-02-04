package ru.yandex.infra.stage.deployunit;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.dto.LogbrokerCommunalTopicRequest;
import ru.yandex.infra.stage.dto.LogbrokerTopicRequest;

public class LogbrokerTopicConfigResolverTestCommunal extends LogbrokerTopicConfigResolverTestBase {

    public static final LogbrokerCommunalTopicRequest DEFAULT_COMMUNAL_TOPIC_REQUEST = LogbrokerCommunalTopicRequest.INSTANCE;

    @Override
    LogbrokerTopicRequest getSpecTopicRequest() {
        return DEFAULT_COMMUNAL_TOPIC_REQUEST;
    }

    @Test
    void getTopicConfigTest() {
        getTopicConfigTestScenario(COMMUNAL_TOPIC_CONFIG);
    }

}
