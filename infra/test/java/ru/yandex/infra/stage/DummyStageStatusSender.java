package ru.yandex.infra.stage;

import ru.yandex.infra.stage.dto.StageStatus;

public class DummyStageStatusSender implements StageStatusSender {
    public StageStatus lastUpdatedStatus;
    public String lastRemovedId;
    public int updatesCount;

    @Override
    public void save(String stageId, StageStatus status) {
        lastUpdatedStatus = status;
        updatesCount++;
    }

    @Override
    public void cancelScheduledStatusUpdate(String stageId) {
        lastRemovedId = stageId;
        updatesCount++;
    }
}
