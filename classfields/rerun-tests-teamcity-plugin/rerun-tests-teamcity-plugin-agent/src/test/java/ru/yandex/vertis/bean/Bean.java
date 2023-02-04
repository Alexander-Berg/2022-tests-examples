package ru.yandex.vertis.bean;

import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.BuildParametersMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Bean {

    public static BuildParametersMap buildParametersSystem(Map<String, String> system) {
        return new BuildParametersMap() {
            @NotNull
            @Override
            public Map<String, String> getEnvironmentVariables() {
                return null;
            }

            @NotNull
            @Override
            public Map<String, String> getSystemProperties() {
                return system;
            }

            @NotNull
            @Override
            public Map<String, String> getAllParameters() {
                return null;
            }
        };
    }

    public static AgentBuildFeature agentBuildFeature(String type) {
        return new AgentBuildFeature() {
            @NotNull
            @Override
            public String getType() {
                return type;
            }

            @NotNull
            @Override
            public Map<String, String> getParameters() {
                return null;
            }
        };
    }
}
