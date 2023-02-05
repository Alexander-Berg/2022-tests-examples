package com.yandex.teamcity.arc;

import com.yandex.teamcity.arc.grpc.ArcGrpcApiCache;
import com.yandex.teamcity.arc.grpc.ArcMockedGrpcApiCache;
import jetbrains.buildServer.vcs.VcsException;
import org.mockito.Mockito;

public class ArcTestUtil {
    private static final ArcGrpcApiCache arcGrpcApiCache = new ArcMockedGrpcApiCache();

    public static ArcGrpcApiCache provideArcGrpcApiCache() throws VcsException {
        Mockito.clearInvocations(arcGrpcApiCache.getArcGrpcApi());
        return arcGrpcApiCache;
    }
}
