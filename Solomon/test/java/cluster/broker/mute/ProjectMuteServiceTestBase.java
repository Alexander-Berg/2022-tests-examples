package ru.yandex.solomon.alert.cluster.broker.mute;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.solomon.alert.api.converters.MuteConverter;
import ru.yandex.solomon.alert.cluster.broker.mute.search.MuteSearch;
import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.mute.domain.MuteStatus;
import ru.yandex.solomon.alert.mute.domain.MuteTestSupport;
import ru.yandex.solomon.alert.protobuf.CreateMuteRequest;
import ru.yandex.solomon.alert.protobuf.CreateMuteResponse;
import ru.yandex.solomon.alert.protobuf.DeleteMuteRequest;
import ru.yandex.solomon.alert.protobuf.DeleteMuteResponse;
import ru.yandex.solomon.alert.protobuf.ListMutesRequest;
import ru.yandex.solomon.alert.protobuf.ListMutesResponse;
import ru.yandex.solomon.alert.protobuf.ReadMuteRequest;
import ru.yandex.solomon.alert.protobuf.ReadMuteResponse;
import ru.yandex.solomon.alert.protobuf.UpdateMuteRequest;
import ru.yandex.solomon.alert.protobuf.UpdateMuteResponse;
import ru.yandex.solomon.alert.protobuf.mute.Mute;
import ru.yandex.solomon.ut.ManualClock;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class ProjectMuteServiceTestBase {
    protected String projectId;
    protected ManualClock clock;
    protected ScheduledExecutorService executorService;
    protected EntitiesDao<ru.yandex.solomon.alert.mute.domain.Mute> dao;
    protected ProjectMuteService service;
    protected MuteConverter converter;

    protected void checkPagedResult(Mute[] expected, ListMutesRequest request) {
        final int size = ThreadLocalRandom.current().nextInt(5, 10);
        List<Mute> buffer = new ArrayList<>(expected.length);
        String token = "";
        do {
            ListMutesResponse response = service.listMutes(request.toBuilder()
                    .setPageSize(size)
                    .setPageToken(token)
                    .build())
                    .join();

            Assert.assertThat(response.getMutesCount(), lessThanOrEqualTo(size));
            buffer.addAll(response.getMutesList());
            token = response.getNextPageToken();
        } while (!token.isEmpty());

        assertArrayEquals(expected, buffer.toArray(new Mute[0]));
    }

    protected List<Mute> createManyMutes() {
        return IntStream.range(1, 100)
                .parallel()
                .mapToObj(index -> CreateMuteRequest.newBuilder()
                        .setMute(randomMute()
                                .toBuilder()
                                .setDescription("Mute - " + index)
                                .setTicketId("TEST-" + ThreadLocalRandom.current().nextInt(1000, 10000))
                                .build())
                        .build())
                .map(request -> service.createMute(request))
                .map(future -> future.thenApply(CreateMuteResponse::getMute))
                .collect(collectingAndThen(toList(), CompletableFutures::joinAll));
    }

    protected Mute successCreate(Mute mute) {
        CreateMuteResponse response = service.createMute(CreateMuteRequest.newBuilder()
                .setMute(mute)
                .build())
                .join();

        return response.getMute();
    }

    protected Mute successRead(String id) {
        ReadMuteResponse response = service.readMute(ReadMuteRequest.newBuilder()
                .setId(id)
                .setProjectId(projectId)
                .build())
                .join();

        return response.getMute();
    }

    protected Mute successUpdate(Mute mute) {
        UpdateMuteResponse response = service.updateMute(UpdateMuteRequest.newBuilder()
                .setMute(mute)
                .build())
                .join();

        return response.getMute();
    }

    protected void successDelete(String id) {
        DeleteMuteResponse response = service.deleteMute(DeleteMuteRequest.newBuilder()
                .setId(id)
                .setProjectId(projectId)
                .build())
                .join();
    }

    protected Mute[] successList(ListMutesRequest request) {
        ListMutesResponse response = service.listMutes(request).join();
        return response.getMutesList().toArray(new Mute[0]);
    }

    protected Mute randomMute() {
        var mute = MuteTestSupport.randomMute()
                .toBuilder()
                .setProjectId(projectId)
                .build();

        return convert(mute);
    }

    protected Mute convert(ru.yandex.solomon.alert.mute.domain.Mute mute) {
        return converter.muteToProto(mute, MuteStatus.UNKNOWN)
                .toBuilder()
                .clearCreatedAt()
                .clearUpdatedAt()
                .clearVersion()
                .build();
    }

    protected void createService() {
        service = new ProjectMuteService(
                projectId,
                clock,
                dao,
                new MuteSearch(MuteConverter.INSTANCE),
                MuteConverter.INSTANCE
        );
    }

    protected void restartService() {
        if (service != null) {
            service.close();
        }

        createService();
        service.run().join();
    }
}
