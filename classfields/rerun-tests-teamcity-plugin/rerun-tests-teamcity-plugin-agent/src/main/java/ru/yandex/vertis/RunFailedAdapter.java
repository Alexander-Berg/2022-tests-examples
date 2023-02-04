package ru.yandex.vertis;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static ru.yandex.vertis.utils.RerunUtils.hasRerunBuildFeature;
import static ru.yandex.vertis.utils.RerunUtils.hasRerunBuildParameter;


public class RunFailedAdapter extends AgentLifeCycleAdapter {

    private Path oldFailedTests;

    public RunFailedAdapter(@NotNull final EventDispatcher<AgentLifeCycleListener> events) {
        events.addListener(this);
    }

    @Override
    public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
        super.beforeRunnerStart(runner);

        if (!hasRerunBuildFeature(runner.getBuild())) {
            return;
        }

        oldFailedTests = runner.getBuild().getBuildTempDirectory().toPath().resolve(RerunConstant.OLD_FAILED);
        BuildProgressLogger buildLogger = runner.getBuild().getBuildLogger();

        if (!shouldRerun(runner)) {
            return;
        }

        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(oldFailedTests, StandardCharsets.UTF_8)) {
            stream.forEach(contentBuilder::append);
        } catch (IOException e) {
            buildLogger.exception(e);
            return;
        }

        if (contentBuilder.toString().equals("")) {
            buildLogger.message("No tests to rerun");
            return;
        }

        String oldArgs = Optional.ofNullable(runner.getRunnerParameters().get("runnerArgs")).orElse("");
        runner.addRunnerParameter("runnerArgs", String.format("%s \"-Dtest=%s\"", oldArgs, contentBuilder.toString()));
        buildLogger.message(" new arguments line is -> " + runner.getRunnerParameters().get("runnerArgs"));
    }

    private boolean shouldRerun(BuildRunnerContext runner) {
        return hasRerunBuildFeature(runner.getBuild())
                && isMavenTestRunner(runner)
                && Files.exists(oldFailedTests)
                && hasRerunBuildParameter(runner);
    }

    private boolean isMavenTestRunner(BuildRunnerContext runnerContext) {
        return runnerContext.getRunType().equals(RerunConstant.MAVEN_RUNNER) &&
                Arrays.asList(runnerContext.getRunnerParameters().get("goals").trim()
                        .split(" ")).contains(RerunConstant.TEST_GOAL);
    }


}
