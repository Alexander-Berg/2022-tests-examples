package ru.yandex.vertis.utils;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import ru.yandex.vertis.RerunConstant;

import java.util.Optional;

public class RerunUtils {

    public static boolean hasRerunBuildFeature(AgentRunningBuild build) {
        return build.getBuildFeatures().stream()
                .anyMatch(feature -> feature.getType().equals(RerunConstant.RUN_TYPE));
    }

    public static boolean hasRerunBuildParameter(BuildRunnerContext runner) {
        return Optional.ofNullable(runner.getConfigParameters().get(RerunConstant.RERUN_PROPERTY))
                .map(Boolean::valueOf).orElse(false);
    }
}
