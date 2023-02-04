package ru.yandex.infra.stage.deployunit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ru.yandex.infra.stage.StageContext;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

public class DummyObjectController<Spec, Status extends ReadyStatus> implements ObjectController<Spec, Status> {
    private Optional<Spec> currentSpec = Optional.empty();
    private Map<String, YTreeNode> currentLabels = new HashMap<>();
    private Map<String, Long> currentTimestampPrerequisites = new HashMap<>();
    private Status status;
    private boolean isShutdown = true;

    public DummyObjectController() {
    }

    @Override
    public void sync(Spec spec, StageContext stageContext, Map<String, YTreeNode> labels, Map<String, Long> timestampPrerequisites,
                     String cluster) {
        currentSpec = Optional.of(spec);
        currentLabels = labels;
        currentTimestampPrerequisites = timestampPrerequisites;
        isShutdown = false;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
    }

    @Override
    public void addStats(DeployUnitStats.Builder builder) {
    }

    public Optional<Spec> getCurrentSpec() {
        return currentSpec;
    }

    public Map<String, YTreeNode> getCurrentLabels() {
        return currentLabels;
    }

    public Map<String, Long> getCurrentTimestampPrerequisites() {
        return currentTimestampPrerequisites;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isShutdown() {
        return isShutdown;
    }
}
