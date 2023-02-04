package ru.yandex.vertis;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.EventDispatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.vertis.bean.Bean;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.vertis.RerunConstant.MAVEN_RUNNER;
import static ru.yandex.vertis.RerunConstant.OLD_FAILED;
import static ru.yandex.vertis.RerunConstant.RUN_TYPE;
import static ru.yandex.vertis.bean.Config.buildConfig;

@RunWith(Parameterized.class)
public class RunFailedMavenArgsTest {

    private static final String RUNNER_ARGS = "runnerArgs";
    private static final String TEST_TO_RERUN = "test1";
    private static final String NEW_MAVEN_ARGS = "%s \"-Dtest=%s\"";

    private Map<String, String> parameters = new HashMap<>();
    private RunFailedAdapter runFailedAdapter;
    private BuildRunnerContext runner;

    @Parameterized.Parameter
    public String mavenArgs;

    @Parameterized.Parameter(1)
    public String oldArgs;

    @Parameterized.Parameter(2)
    public int count;

    @Parameterized.Parameters(name = "«{index}» {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {null, "", 1},
                {"", "", 1},
                {"-DskipTests", "-DskipTests", 1},
                {"-X", "-X", 1},
                {"| grep «^&*()#@$!", "| grep «^&*()#@$!", 1},
        });
    }

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void buildWithRerunFeature() throws IOException {
        AgentRunningBuild build = mock(AgentRunningBuild.class);
        when(build.getBuildTempDirectory()).thenReturn(tmpFolder.getRoot());
        when(build.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));
        when(build.getBuildFeatures()).thenReturn(Collections.singletonList(Bean.agentBuildFeature(RUN_TYPE)));
        when(build.getSharedBuildParameters()).thenReturn(Bean.buildParametersSystem(buildConfig()));
        when(build.getBuildTypeExternalId()).thenReturn("myType");

        runFailedAdapter = new RunFailedAdapter(mock(EventDispatcher.class));

        runner = mock(BuildRunnerContext.class);
        when(runner.getBuild()).thenReturn(build);
        when(runner.getRunType()).thenReturn(MAVEN_RUNNER);
        when(runner.getConfigParameters()).thenReturn(Collections.singletonMap("rerun.test.property", "true"));

        Files.write(tmpFolder.newFile(OLD_FAILED).toPath(), TEST_TO_RERUN.getBytes());

        parameters.put("goals", "clean test");
    }


    @Test
    public void shouldParseGoals() {
        parameters.put(RUNNER_ARGS, mavenArgs);
        when(runner.getRunnerParameters()).thenReturn(parameters);

        runFailedAdapter.beforeRunnerStart(runner);
        verify(runner, times(count))
                .addRunnerParameter(RUNNER_ARGS, format(NEW_MAVEN_ARGS, oldArgs, TEST_TO_RERUN));
    }


}
