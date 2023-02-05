/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.operation.network;

import androidx.annotation.NonNull;

import com.yandex.datasync.YDSContext;
import com.yandex.datasync.internal.model.response.DatabaseDto;
import com.yandex.datasync.internal.model.response.SnapshotResponse;
import com.yandex.datasync.WrappersObserver;
import com.yandex.datasync.internal.observer.RawDataObserver;

import java.util.List;

public class MockRawDataObserver implements RawDataObserver {

    private List<DatabaseDto> databaseList;

    private SnapshotResponse snapshot;

    private DatabaseDto database;

    private Exception exception;

    private YDSContext databaseContext;

    private String databaseId;

    private String collectionId;

    @Override
    public void addObserver(@NonNull final WrappersObserver observer) {

    }

    @Override
    public void removeObserver(@NonNull final WrappersObserver observer) {

    }

    @Override
    public void notifyDatabaseListRetrieved(@NonNull final YDSContext databaseContext,
                                            @NonNull final List<DatabaseDto> databaseList) {
        this.databaseContext = databaseContext;
        this.databaseList = databaseList;
    }

    @Override
    public void notifySnapshotRetrieved(@NonNull final YDSContext context,
                                        @NonNull final String databaseId,
                                        @NonNull final SnapshotResponse snapshot) {

        this.snapshot = snapshot;
    }

    @Override
    public void notifyCollectionRetrieved(@NonNull final YDSContext context,
                                          @NonNull final String databaseId,
                                          @NonNull final String collectionId,
                                          @NonNull final SnapshotResponse snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public void notifyCollectionReseted(@NonNull final YDSContext databaseContext,
                                        @NonNull final String databaseId,
                                        @NonNull final String collectionId) {
        this.databaseContext = databaseContext;
        this.databaseId = databaseId;
        this.collectionId = collectionId;
    }

    @Override
    public void notifyDatabaseReseted(@NonNull final YDSContext databaseContext,
                                      @NonNull final String databaseId) {
        this.databaseContext = databaseContext;
        this.databaseId = databaseId;
    }

    @Override
    public void notifyDatabaseSynced(@NonNull final YDSContext databaseContext,
                                     @NonNull final DatabaseDto database) {
        this.database = database;
    }

    @Override
    public void notifyError(@NonNull final Exception exception) {
        this.exception = exception;
    }

    @Override
    public void notifyFullSyncSuccess(@NonNull final YDSContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    @Override
    public void notifyFullSyncFailed(@NonNull final YDSContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    @Override
    public void notifyDatabaseCreated(@NonNull final YDSContext databaseContext,
                                      @NonNull final DatabaseDto database) {
        this.databaseContext = databaseContext;
        this.database = database;
    }

    @Override
    public void notifyDatabaseInfoRetrieved(@NonNull final YDSContext databaseContext,
                                            @NonNull final DatabaseDto database) {
        this.databaseContext = databaseContext;
        this.database = database;
    }

    public List<DatabaseDto> getDatabaseList() {
        return databaseList;
    }

    public SnapshotResponse getSnapshot() {
        return snapshot;
    }

    public DatabaseDto getDatabase() {
        return database;
    }

    public Exception getException() {
        return exception;
    }

    public YDSContext getDatabaseContext() {
        return databaseContext;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public String getCollectionId() {
        return collectionId;
    }
}
