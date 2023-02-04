package ru.yandex.vertis;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.vertis.bean.Bean;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.vertis.RerunConstant.RUN_TYPE;
import static ru.yandex.vertis.bean.Config.buildConfig;
import static ru.yandex.vertis.bean.Config.tcConfig;
import static ru.yandex.vertis.utils.RerunUtils.hasRerunBuildFeature;

public class RunTypeTest {

    private AgentRunningBuild build;
    private Map<String, String> tcConfig;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().notifier(new ConsoleNotifier(true)));

    @Before
    public void buildWithRerunFeature() {
        tcConfig = tcConfig(wireMockRule);

        build = mock(AgentRunningBuild.class);
        when(build.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));
        when(build.getSharedBuildParameters()).thenReturn(Bean.buildParametersSystem(buildConfig()));
        when(build.getBuildTypeExternalId()).thenReturn("myType");
    }

    @Test
    public void shouldNotRunWithoutRunType() {
        when(build.getBuildFeatures()).thenReturn(Collections.singletonList(Bean.agentBuildFeature("otherType")));
        assertThat("Не должны запускать без фичи " + RUN_TYPE, hasRerunBuildFeature(build), is(false));
    }

    @Test
    public void shouldRunWithRunType() {
        when(build.getBuildFeatures()).thenReturn(Collections.singletonList(Bean.agentBuildFeature(RUN_TYPE)));
        assertThat("Должны запускать с фичей " + RUN_TYPE, hasRerunBuildFeature(build), is(true));
    }

}
