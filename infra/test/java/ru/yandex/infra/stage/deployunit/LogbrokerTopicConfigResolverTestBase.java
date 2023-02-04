package ru.yandex.infra.stage.deployunit;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.LogbrokerConfig;
import ru.yandex.infra.stage.dto.LogbrokerTopicConfig;
import ru.yandex.infra.stage.dto.LogbrokerTopicRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

abstract class LogbrokerTopicConfigResolverTestBase {

    private static final String COMMUNAL_TVM_TOKEN = "communaltopictvmtoken";

    static final LogbrokerTopicConfig COMMUNAL_TOPIC_CONFIG = new LogbrokerTopicConfig(
        TestData.LOGBROKER_TOPIC_DESCRIPTION, COMMUNAL_TVM_TOKEN
    );

    private LogbrokerTopicConfigResolver topicConfigResolver;

    DeployUnitDescription deployUnitDescription;

    @BeforeEach
    void initResolver() {
        topicConfigResolver = new LogbrokerTopicConfigResolverImpl(
                COMMUNAL_TOPIC_CONFIG.getTopicDescription(),
                COMMUNAL_TVM_TOKEN
        );

        var deployUnitSpec = mock(DeployUnitSpec.class);
        setSpecTopicRequest(deployUnitSpec, getSpecTopicRequest());

        deployUnitDescription = new DeployUnitDescription(
            TestData.DEFAULT_UNIT_CONTEXT.getFullDeployUnitId(), deployUnitSpec
        );
    }

    abstract LogbrokerTopicRequest getSpecTopicRequest();

    private static void setSpecTopicRequest(DeployUnitSpec spec, LogbrokerTopicRequest topicRequest) {
        var logbrokerConfig = mock(LogbrokerConfig.class);
        when(spec.getLogbrokerConfig()).thenReturn(logbrokerConfig);
        when(logbrokerConfig.getTopicRequest()).thenReturn(topicRequest);
    }

    void getTopicConfigTestScenario(LogbrokerTopicConfig expectedTopicConfig) {
        LogbrokerTopicConfig actualTopicConfig = topicConfigResolver.get(deployUnitDescription);
        assertThatEquals(actualTopicConfig, expectedTopicConfig);
    }
}
