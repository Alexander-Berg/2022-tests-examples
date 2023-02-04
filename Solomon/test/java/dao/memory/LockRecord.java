package ru.yandex.solomon.alert.dao.memory;

import java.time.Instant;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.MoreObjects;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class LockRecord {
    private final String alertId;
    private final String lockedBy;
    private final Instant lockedWhen;
    private final Instant expirationTime;

    public LockRecord(String alertId, String lockedBy, Instant lockedWhen, Instant expirationTime) {
        this.alertId = alertId;
        this.lockedBy = lockedBy;
        this.lockedWhen = lockedWhen;
        this.expirationTime = expirationTime;
    }

    public String getAlertId() {
        return alertId;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public Instant getLockedWhen() {
        return lockedWhen;
    }

    public Instant getExpirationTime() {
        return expirationTime;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("alertId", alertId)
                .add("lockedBy", lockedBy)
                .add("lockedWhen", lockedWhen)
                .add("expirationTime", expirationTime)
                .toString();
    }
}
