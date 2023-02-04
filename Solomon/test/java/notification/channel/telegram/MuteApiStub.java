package ru.yandex.solomon.alert.notification.channel.telegram;

import java.util.concurrent.CompletableFuture;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.alert.client.MuteApi;
import ru.yandex.solomon.alert.cluster.broker.mute.ProjectMuteService;
import ru.yandex.solomon.alert.protobuf.CreateMuteRequest;
import ru.yandex.solomon.alert.protobuf.CreateMuteResponse;
import ru.yandex.solomon.alert.protobuf.DeleteMuteRequest;
import ru.yandex.solomon.alert.protobuf.DeleteMuteResponse;
import ru.yandex.solomon.alert.protobuf.ListMutesRequest;
import ru.yandex.solomon.alert.protobuf.ListMutesResponse;
import ru.yandex.solomon.alert.protobuf.ReadMuteRequest;
import ru.yandex.solomon.alert.protobuf.ReadMuteResponse;
import ru.yandex.solomon.alert.protobuf.ReadMuteStatsRequest;
import ru.yandex.solomon.alert.protobuf.ReadMuteStatsResponse;
import ru.yandex.solomon.alert.protobuf.UpdateMuteRequest;
import ru.yandex.solomon.alert.protobuf.UpdateMuteResponse;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class MuteApiStub implements MuteApi {
    private final ProjectMuteService muteService;

    public MuteApiStub(ProjectMuteService muteService) {
        this.muteService = muteService;
    }

    @Override
    public CompletableFuture<CreateMuteResponse> createMute(CreateMuteRequest request, long ignore) {
        return muteService.createMute(request);
    }

    @Override
    public CompletableFuture<ReadMuteResponse> readMute(ReadMuteRequest request, long ignore) {
        return muteService.readMute(request);
    }

    @Override
    public CompletableFuture<UpdateMuteResponse> updateMute(UpdateMuteRequest request, long ignore) {
        return muteService.updateMute(request);
    }

    @Override
    public CompletableFuture<DeleteMuteResponse> deleteMute(DeleteMuteRequest request, long ignore) {
        return muteService.deleteMute(request);
    }

    @Override
    public CompletableFuture<ListMutesResponse> listMutes(ListMutesRequest request, long ignore) {
        return muteService.listMutes(request);
    }

    @Override
    public CompletableFuture<ReadMuteStatsResponse> readMuteStats(ReadMuteStatsRequest request, long ignore) {
        return muteService.readMuteStats(request);
    }
}
