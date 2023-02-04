package ru.yandex.infra.stage.deployunit;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.infra.stage.podspecs.patcher.endpoint_set_liveness.EndpointSetLivenessPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.endpoint_set_port.EndpointSetPortPatcherV1;
import ru.yandex.infra.stage.podspecs.revision.RevisionsHolder;
import ru.yandex.yp.client.api.DataModel;

public class DummyRevisionHolder  implements RevisionsHolder<DataModel.TEndpointSetSpec.Builder> {
    @Override
    public Optional<List<SpecPatcher<DataModel.TEndpointSetSpec.Builder>>> getPatchersFor(int revisionId) {
        return Optional.of(ImmutableList.of(new EndpointSetPortPatcherV1(),
                new EndpointSetLivenessPatcherV1()));
    }

    @Override
    public boolean containsRevision(int revisionId) {
        return true;
    }
}

