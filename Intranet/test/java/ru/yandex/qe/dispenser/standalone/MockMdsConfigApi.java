package ru.yandex.qe.dispenser.standalone;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import ru.yandex.qe.dispenser.domain.mds.MdsConfigApi;
import ru.yandex.qe.dispenser.domain.mds.MdsMarketingStorage;
import ru.yandex.qe.dispenser.domain.mds.MdsRawStorage;
import ru.yandex.qe.dispenser.domain.mds.MdsRawStorageByDc;
import ru.yandex.qe.dispenser.domain.mds.StorageType;

public class MockMdsConfigApi implements MdsConfigApi {

    private static final Map<StorageType, Double> DEFAULT = ImmutableMap.of(StorageType.S3, 3d, StorageType.MDS, 2d, StorageType.AVATARS, 4d);

    private final Map<StorageType, Double> multiplierByStorageType = new HashMap<>(DEFAULT);
    private final Map<Long, MdsRawStorageByDc> rawStorageByDcByChangeId = new HashMap<>();
    private boolean passThrough = false;
    private boolean fail = false;

    @Override
    public List<MdsRawStorage> calculateRawStorage(final Collection<MdsMarketingStorage> storage) {
        return storage.stream()
                .map(marketingStorage -> new MdsRawStorage(marketingStorage.getId(),
                        (long) Math.ceil(marketingStorage.getStorage() * multiplierByStorageType.get(marketingStorage.getType()))))
                .collect(Collectors.toList());
    }

    @Override
    public List<MdsRawStorageByDc> calculateRawStorageByDc(final Collection<MdsMarketingStorage> storage) {
        if (fail) {
            throw new IllegalStateException("Fail");
        }

        if (passThrough) {
            return storage.stream().map(marketing -> new MdsRawStorageByDc(marketing.getId(), List.of(
                    new MdsRawStorageByDc.DcStorage("MAN", marketing.getStorage()),
                    new MdsRawStorageByDc.DcStorage("SAS", marketing.getStorage()),
                    new MdsRawStorageByDc.DcStorage("VLA", marketing.getStorage()),
                    new MdsRawStorageByDc.DcStorage("IVA", marketing.getStorage()),
                    new MdsRawStorageByDc.DcStorage("MYT", marketing.getStorage()))))
                    .collect(Collectors.toList());
        } else {
            return storage.stream()
                    .map(s -> rawStorageByDcByChangeId.get(s.getId()))
                    .collect(Collectors.toList());
        }
    }

    public void setMultipliers(final double mdsMultiplier, final double s3Multiplier, final double avatarsMultiplier) {
        multiplierByStorageType.put(StorageType.MDS, mdsMultiplier);
        multiplierByStorageType.put(StorageType.S3, s3Multiplier);
        multiplierByStorageType.put(StorageType.AVATARS, avatarsMultiplier);
    }

    public void setDefaultMultipliers() {
        multiplierByStorageType.putAll(DEFAULT);
    }

    public void setRawStorageByDcByChangeId(final List<MdsRawStorageByDc> storage) {
        this.rawStorageByDcByChangeId.clear();
        for (final MdsRawStorageByDc mdsRawStorageByDc : storage) {
            this.rawStorageByDcByChangeId.put(mdsRawStorageByDc.getId(), mdsRawStorageByDc);
        }
    }

    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }
}
