package ru.yandex.vertis;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.EventDispatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.yandex.vertis.bean.Bean;

import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.vertis.RerunConstant.OLD_FAILED;
import static ru.yandex.vertis.RerunConstant.RUN_TYPE;
import static ru.yandex.vertis.bean.Config.buildConfig;

public class RunFailedTest {

    public static final String RUNNER_TYPE = "Maven2";
    public static final String FAILED_TESTS = "data";
    public static final String OLD_ARGS = "sdf";
    public static final String RUNNER_ARGS = "runnerArgs";
    private String artUrl;
    private AgentRunningBuild build;
    private Map<String, String> parameters = new HashMap<>();
    private RunFailedAdapter runFailedAdapter;
    private BuildRunnerContext runner;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void buildWithRerunFeature() {
        build = mock(AgentRunningBuild.class);
        when(build.getBuildTempDirectory()).thenReturn(tmpFolder.getRoot());
        when(build.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));
        when(build.getBuildFeatures()).thenReturn(Collections.singletonList(Bean.agentBuildFeature(RUN_TYPE)));
        when(build.getSharedBuildParameters()).thenReturn(Bean.buildParametersSystem(buildConfig()));
        when(build.getBuildTypeExternalId()).thenReturn("myType");

        parameters.put("goals", "clean test");
        parameters.put(RUNNER_ARGS, OLD_ARGS);

        runner = mock(BuildRunnerContext.class);
        when(runner.getBuild()).thenReturn(build);
        when(runner.getRunType()).thenReturn(RUNNER_TYPE);
        when(runner.getRunnerParameters()).thenReturn(parameters);
        when(runner.getConfigParameters()).thenReturn(Collections.singletonMap("rerun.test.property", "true"));

        runFailedAdapter = new RunFailedAdapter(mock(EventDispatcher.class));
    }

    @Test
    public void shouldAddFailedTestsFromFile() throws Exception {
        Files.write(tmpFolder.newFile(OLD_FAILED).toPath(), FAILED_TESTS.getBytes());
        runFailedAdapter.beforeRunnerStart(runner);
        verify(runner, times(1)).addRunnerParameter(RUNNER_ARGS, format("%s \"-Dtest=%s\"", OLD_ARGS, FAILED_TESTS));
    }

    @Test
    public void shouldNotAddIfFileNotExist() {
        runFailedAdapter.beforeRunnerStart(runner);
        verify(runner, times(0)).addRunnerParameter(any(), any());
    }

    @Test
    public void shouldAddIfFileIsEmpty() throws Exception {
        Files.write(tmpFolder.newFile(OLD_FAILED).toPath(), "".getBytes());
        runFailedAdapter.beforeRunnerStart(runner);
        verify(runner, times(0)).addRunnerParameter(any(), any());
    }
}
