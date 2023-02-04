package ru.yandex.vertis;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.EventDispatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.yandex.vertis.bean.Bean;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.vertis.RerunConstant.FAILED;
import static ru.yandex.vertis.RerunConstant.TC_HIDDEN_DIR;
import static ru.yandex.vertis.bean.Config.buildConfig;
import static ru.yandex.vertis.bean.Config.tcConfig;

public class CopyFailedConditionTest {

    private static final String DATA = "test1,test2";

    private AgentRunningBuild build;
    private Map<String, String> tcConfig;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration()
            .notifier(new ConsoleNotifier(true)).port(16009));

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void buildWithoutRerunFeature() {
        tcConfig = tcConfig(wireMockRule);

        build = mock(AgentRunningBuild.class);
        when(build.getBuildTempDirectory()).thenReturn(tmpFolder.getRoot());
        when(build.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));
        when(build.getSharedBuildParameters()).thenReturn(Bean.buildParametersSystem(buildConfig()));
        when(build.getBuildTypeExternalId()).thenReturn("myType");
    }

    @Test
    public void shouldNotCopyWithoutFeature() {
        String artUrl = String.format("/repository/download/myType/%s/%s", ".lastFinished", TC_HIDDEN_DIR + "/" + FAILED);
        wireMockRule.stubFor(get(artUrl).willReturn(ok().withBody(DATA)));

        when(build.getSharedConfigParameters()).thenReturn(tcConfig);

        CopyFailedAdapter copyFailedAdapter = new CopyFailedAdapter(mock(EventDispatcher.class));
        copyFailedAdapter.preparationFinished(build);

        wireMockRule.verify(0, getRequestedFor(urlEqualTo(artUrl)));
    }

}
