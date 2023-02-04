package ru.yandex.vertis;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static ru.yandex.vertis.utils.RerunUtils.hasRerunBuildFeature;

public class CopyFailedAdapter extends AgentLifeCycleAdapter {

    private AgentRunningBuild runningBuild;
    private Path tempOldFailed;

    public CopyFailedAdapter(@NotNull final EventDispatcher<AgentLifeCycleListener> events) {
        events.addListener(this);
    }

    @Override
    public void preparationFinished(@NotNull AgentRunningBuild runningBuild) {
        super.preparationFinished(runningBuild);

        if (!hasRerunBuildFeature(runningBuild)) {
            return;
        }

        this.runningBuild = runningBuild;
        this.tempOldFailed = runningBuild.getBuildTempDirectory().toPath().resolve(RerunConstant.OLD_FAILED);

        try {
            copyFailedFromBuild();
        } catch (Exception e) {
            getLogger().message("Couldn't copy artifact...");
            return;
        }
        getLogger().message(format("Downloaded to [%s]", tempOldFailed));
    }

    private void copyFailedFromBuild() throws Exception {
        String buildNumber = Optional.ofNullable(
                getBuildConfigParameter(RerunConstant.BUILD_NUMBER_PROPERTY))
                .filter(number -> !number.isEmpty())
                .orElse(".lastFinished");

        StringBuilder lasFailed = new StringBuilder();
        lasFailed.append(format("%s/repository/download/%s/%s/%s",
                getTeamcityBaseUrl(), runningBuild.getBuildTypeExternalId(),
                buildNumber, RerunConstant.TEAMCITY_FAILED_ARTIFACT_PATH));

        String branch = runningBuild.getSharedConfigParameters().get("teamcity.build.branch");
        if (Objects.nonNull(branch)) {
            lasFailed.append(format("?branch=%s", branch));
        }

        getLogger().message(format("Search failed tests [%s] ...", lasFailed.toString()));
        if (!Files.exists(tempOldFailed)) {
            Files.createFile(tempOldFailed);
        }

        String password = getServerAuthentication();
        String encoding = Base64.getUrlEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));

        HttpURLConnection connection = (HttpURLConnection) new URL(lasFailed.toString()).openConnection();
        connection.setRequestProperty("Authorization", "Basic " + encoding);
        connection.connect();

        boolean isArtifactMissing = connection.getResponseCode() != 200;
        if (isArtifactMissing) {
            throw new IllegalStateException(format("Couldn't connect to [%s] ...", lasFailed.toString()));
        }

        getLogger().message(format("Coping artifact [%s] ...", lasFailed.toString()));
        try (InputStream stream = connection.getInputStream()) {
            Files.copy(stream, tempOldFailed, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @NotNull
    private String getTeamcityBaseUrl() {
        String baseUrl = runningBuild.getSharedConfigParameters().get("teamcity.serverUrl");
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 2) : baseUrl;
    }

    @NotNull
    private String getServerAuthentication() {
        return format("%s:%s",
                runningBuild.getSharedBuildParameters().getSystemProperties().get("teamcity.auth.userId"),
                runningBuild.getSharedBuildParameters().getSystemProperties().get("teamcity.auth.password")
        );
    }

    private BuildProgressLogger getLogger() {
        return runningBuild.getBuildLogger();
    }

    private String getBuildConfigParameter(String key) {
        return runningBuild.getSharedConfigParameters().get(key);
    }
}
