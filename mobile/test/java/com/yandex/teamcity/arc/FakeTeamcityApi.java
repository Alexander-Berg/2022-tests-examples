package com.yandex.teamcity.arc;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.vcs.ModificationData;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsFileData;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;

public class FakeTeamcityApi implements TeamCityApi {

    @Override
    @NotNull
    public Map<String, String> getCurrentState(@NotNull VcsRoot repository,
                                               @NotNull TeamcityPropertiesBridge bridge) throws VcsException {
        return Collections.emptyMap();
    }

    @Override
    public List<ModificationData> collectChangeOverGrpc(@NotNull VcsRoot repository,
                                                        @NotNull Map<String, String> fromRevisions,
                                                        @NotNull Map<String, String> toRevisions,
                                                        TeamcityPropertiesBridge bridge) {
        return Collections.emptyList();
    }

    @Override
    public List<ModificationData> collectChangeParallel(@NotNull VcsRoot repository,
                                                 @NotNull Map<String, String> fromRevisions,
                                                 @NotNull Map<String, String> toRevisions,
                                                 @NotNull TeamcityPropertiesBridge bridge) throws VcsException {
        return Collections.emptyList();
    }

    @Override
    public byte @NotNull [] getContentOverGrpc(@NotNull String filePath, @NotNull String revision, long limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Collection<VcsFileData> listFilesOverGrpc(@NotNull String directoryPath) throws VcsException {
        throw new UnsupportedOperationException();
    }
}
