package ru.yandex.market.service.sync;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import ru.yandex.market.utils.StringUtils;
import ru.yandex.market.data.Syncable;

class TestSynchronizer extends AbstractSynchronizer<Long, TestSynchronizer.TestEntity> {

    private List<TestEntity> localEntities;

    public TestSynchronizer(final Context context) {
        super(context);
    }

    public void setLocalEntities(
            final List<TestEntity> localEntities) {
        this.localEntities = localEntities;
    }

    @Override
    protected List<TestEntity> getLocalEntities() {
        return localEntities;
    }

    @Override
    protected void saveEntity(@NonNull final TestEntity entity) {

    }

    @Override
    protected void uploadEntity(@NonNull final TestEntity entity,
            final FinishListener<TestEntity> listener) {

    }

    @Override
    protected void deleteEntityFromDevice(final TestEntity entity) {

    }

    @Override
    protected void deleteEntityFromServer(final TestEntity entity,
            final FinishListener<TestEntity> listener) {

    }

    @Override
    protected void updateEntity(final TestEntity entity,
            final FinishListener<TestEntity> listener) {

    }

    @Override
    protected BidirectionalSyncHandler<TestEntity> getBidirectionalSyncHandler() {
        return (server, local) -> {
            if (local.isSyncDirty()) {
                return BidirectionalSyncType.SEND_TO_SERVER;
            } else if (StringUtils.equalsIgnoreCase(server.difference, local.difference)) {
                return BidirectionalSyncType.NONE;
            } else {
                return BidirectionalSyncType.RECEIVE_FROM_SERVER;
            }
        };
    }

    static class TestEntity extends Syncable<Long> {

        String difference;

        TestEntity(Long local, Long server, boolean dirty, String difference) {
            setId(local);
            setServerId(server);
            setSyncDirty(dirty);
            this.difference = difference;
        }
    }
}
