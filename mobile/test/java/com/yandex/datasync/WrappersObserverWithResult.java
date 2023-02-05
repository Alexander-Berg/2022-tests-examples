/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync;

import androidx.annotation.NonNull;

import com.yandex.datasync.wrappedModels.Collection;
import com.yandex.datasync.wrappedModels.DatabaseList;
import com.yandex.datasync.wrappedModels.Database;
import com.yandex.datasync.wrappedModels.Error;
import com.yandex.datasync.wrappedModels.Snapshot;

public class WrappersObserverWithResult implements WrappersObserver {

    private DatabaseList databaseList;

    private Snapshot snapshot;

    private Database database;

    private Error error;

    private Collection collection;

    private YDSContext databaseContext;

    private String databaseId;

    private String collectionId;

    private long databaseRevision;

    @Override
    public void notifyDatabaseListRetrieved(@NonNull final DatabaseList databaseList) {
        this.databaseList = databaseList;
    }

    @Override
    public void notifySnapshotRetrieved(@NonNull final Snapshot snapshot, final long revision) {
        this.snapshot = snapshot;
        this.databaseRevision = revision;
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
    public void notifyDatabaseSynced(@NonNull final Database database) {
        this.database = database;
    }

    @Override
    public void notifyError(@NonNull final Error error) {
        this.error = error;
    }

    @Override
    public void notifyCollectionRetrieved(@NonNull final Collection collection,
                                          final long revision) {
        this.collection = collection;
        this.databaseRevision = revision;
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
    public void notifyDatabaseInfoRetrieved(@NonNull final Database databaseWrapper) {
        this.database = databaseWrapper;
    }

    @Override
    public void notifyDatabaseCreated(@NonNull final Database databaseWrapper) {
        this.database = databaseWrapper;
    }

    public DatabaseList getDatabaseList() {
        return databaseList;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public Error getError() {
        return error;
    }

    public Collection getCollection() {
        return collection;
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

    public Database getDatabase() {
        return database;
    }

    public long getDatabaseRevision() {
        return databaseRevision;
    }
}
