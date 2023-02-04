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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.vertis.RerunConstant.RUN_TYPE;
import static ru.yandex.vertis.bean.Config.buildConfig;

public class RunFailedConditionTest {

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
        parameters.put("runnerArgs", "some args");

        runFailedAdapter = new RunFailedAdapter(mock(EventDispatcher.class));
    }

    @Test
    public void shouldRunOnlyIfMaven2() {
        runner = mock(BuildRunnerContext.class);
        when(runner.getBuild()).thenReturn(build);
        when(runner.getRunnerParameters()).thenReturn(parameters);
        when(runner.getConfigParameters()).thenReturn(Collections.singletonMap("rerun.test.property", "true"));

        when(runner.getRunType()).thenReturn("NOT_MAVEN2");

        runFailedAdapter.beforeRunnerStart(runner);
        verify(runner, times(0)).addRunnerParameter(any(), any());
    }

    @Test
    public void shouldRunOnlyIfTestRerunProperty() {
        runner = mock(BuildRunnerContext.class);
        when(runner.getBuild()).thenReturn(build);
        when(runner.getRunnerParameters()).thenReturn(parameters);
        when(runner.getRunType()).thenReturn("Maven2");

        when(runner.getConfigParameters()).thenReturn(Collections.singletonMap("rerun.test.property", "false"));

        runFailedAdapter.beforeRunnerStart(runner);
        verify(runner, times(0)).addRunnerParameter(any(), any());
    }

}
