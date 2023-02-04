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

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.vertis.RerunConstant.FAILED;
import static ru.yandex.vertis.RerunConstant.OLD_FAILED;
import static ru.yandex.vertis.RerunConstant.RUN_TYPE;
import static ru.yandex.vertis.RerunConstant.TC_HIDDEN_DIR;
import static ru.yandex.vertis.bean.Config.buildConfig;
import static ru.yandex.vertis.bean.Config.tcConfig;

public class CopyFailedTest {

    private static final String NUMERIC_BUILD_NUMBER = "123456789";
    private static final String BRANCH_NAME = "branchName";
    private static final String DATA = "test1,test2";

    private String artUrl;
    private AgentRunningBuild build;
    private CopyFailedAdapter copyFailedAdapter;
    private Map<String, String> tcConfig;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration()
            .notifier(new ConsoleNotifier(true)).port(16009));

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void buildWithRerunFeature() {
        tcConfig = tcConfig(wireMockRule);

        build = mock(AgentRunningBuild.class);
        when(build.getBuildTempDirectory()).thenReturn(tmpFolder.getRoot());
        when(build.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));
        when(build.getBuildFeatures()).thenReturn(Collections.singletonList(Bean.agentBuildFeature(RUN_TYPE)));
        when(build.getSharedBuildParameters()).thenReturn(Bean.buildParametersSystem(buildConfig()));
        when(build.getBuildTypeExternalId()).thenReturn("myType");

        copyFailedAdapter = new CopyFailedAdapter(mock(EventDispatcher.class));
    }

    @Test
    public void shouldCopyFromLatestBuild() throws IOException {
        artUrl = String.format("/repository/download/myType/%s/%s", ".lastFinished", TC_HIDDEN_DIR + "/" + FAILED);
        wireMockRule.stubFor(get(artUrl).willReturn(ok().withBody(DATA)));

        when(build.getSharedConfigParameters()).thenReturn(tcConfig);

        copyFailedAdapter = new CopyFailedAdapter(mock(EventDispatcher.class));
        copyFailedAdapter.preparationFinished(build);

        wireMockRule.verify(getRequestedFor(urlEqualTo(artUrl)));
        shouldSeeFile();
    }

    @Test
    public void shouldCopyFromCustomBuild() throws IOException {
        artUrl = String.format("/repository/download/myType/%s/%s", NUMERIC_BUILD_NUMBER, TC_HIDDEN_DIR + "/" + FAILED);
        wireMockRule.stubFor(get(artUrl).willReturn(ok().withBody(DATA)));

        tcConfig.put("rerun.test.build", NUMERIC_BUILD_NUMBER);
        when(build.getSharedConfigParameters()).thenReturn(tcConfig);

        copyFailedAdapter = new CopyFailedAdapter(mock(EventDispatcher.class));
        copyFailedAdapter.preparationFinished(build);

        wireMockRule.verify(getRequestedFor(urlEqualTo(artUrl)));
        shouldSeeFile();
    }

    @Test
    public void shouldCopyFromLatestBranch() throws IOException {
        artUrl = String.format("/repository/download/myType/%s/%s?branch=%s", ".lastFinished",
                TC_HIDDEN_DIR + "/" + FAILED, BRANCH_NAME);
        wireMockRule.stubFor(get(artUrl).willReturn(ok().withBody(DATA)));

        tcConfig.put("teamcity.build.branch", BRANCH_NAME);
        when(build.getSharedConfigParameters()).thenReturn(tcConfig);

        copyFailedAdapter = new CopyFailedAdapter(mock(EventDispatcher.class));
        copyFailedAdapter.preparationFinished(build);

        wireMockRule.verify(getRequestedFor(urlEqualTo(artUrl)));
        shouldSeeFile();
    }

    @Test
    public void shouldCopyWithCustomBuildAndBranch() throws IOException {
        artUrl = String.format("/repository/download/myType/%s/%s?branch=%s", NUMERIC_BUILD_NUMBER,
                TC_HIDDEN_DIR + "/" + FAILED, BRANCH_NAME);
        wireMockRule.stubFor(get(artUrl).willReturn(ok().withBody(DATA)));

        tcConfig.put("teamcity.build.branch", BRANCH_NAME);
        tcConfig.put("rerun.test.build", NUMERIC_BUILD_NUMBER);
        when(build.getSharedConfigParameters()).thenReturn(tcConfig);

        copyFailedAdapter = new CopyFailedAdapter(mock(EventDispatcher.class));
        copyFailedAdapter.preparationFinished(build);

        wireMockRule.verify(getRequestedFor(urlEqualTo(artUrl)));
        shouldSeeFile();
    }

    private void shouldSeeFile() throws IOException {
        assertThat("Создался файл с тестами", tmpFolder.getRoot().toPath().resolve(OLD_FAILED).toFile(),
                anExistingFile());
        List<String> lines = Files.readAllLines(tmpFolder.getRoot().toPath().resolve(OLD_FAILED));
        assertThat("В файле нужная строчка", lines, is(asList(DATA)));
    }
}
