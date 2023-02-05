/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.internal.observer;

import androidx.annotation.NonNull;

import com.yandex.datasync.WrappersObserver;
import com.yandex.datasync.YDSContext;
import com.yandex.datasync.wrappedModels.Collection;
import com.yandex.datasync.wrappedModels.Database;
import com.yandex.datasync.wrappedModels.DatabaseList;
import com.yandex.datasync.wrappedModels.Error;
import com.yandex.datasync.wrappedModels.Snapshot;

import org.junit.Test;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class RawDataObserverImplTest {

    private static final int THREADS_COUNT = 1000;

    private CountDownLatch lock;

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testMultithreading() throws InterruptedException {

        lock = new CountDownLatch(THREADS_COUNT * 2);

        final RawDataObserverImpl rawDataObserver = new RawDataObserverImpl(null, null, null, null);
        final LinkedBlockingQueue<WrappersObserver> wrappresQueue = new LinkedBlockingQueue<>();

        new Thread(() -> {
            for (int i = 0; i < THREADS_COUNT; i++) {
                new Thread(new AddRunnable(rawDataObserver, wrappresQueue)).start();
            }
        }).start();

        new Thread(() -> {
            for (int i = 0; i < THREADS_COUNT; i++) {
                new Thread(new RemoveRunnable(rawDataObserver, wrappresQueue)).start();
            }
        }).start();

        lock.await(10, TimeUnit.SECONDS);
        assertThat(lock.getCount(), is(0l));
        assertThat(lock.getCount(), is(((long) rawDataObserver.getObservableCount())));
    }

    private class AddRunnable implements Runnable {

        @NonNull
        private final RawDataObserver observer;

        @NonNull
        private final Queue<WrappersObserver> wrappersQueue;

        private AddRunnable(@NonNull final RawDataObserver observer,
                            @NonNull final Queue<WrappersObserver> wrappersQueue) {
            this.observer = observer;
            this.wrappersQueue = wrappersQueue;
        }

        @Override
        public void run() {
            final WrappersObserver wrappersObserver = new SimpleWrappersObserver();

            observer.addObserver(wrappersObserver);
            wrappersQueue.add(wrappersObserver);
            System.out.println("add: " + wrappersObserver.hashCode());
            lock.countDown();
        }
    }


    private class RemoveRunnable implements Runnable {

        @NonNull
        private final RawDataObserver observer;

        @NonNull
        private final LinkedBlockingQueue<WrappersObserver> wrappersQueue;

        private RemoveRunnable(@NonNull final RawDataObserver observer,
                               @NonNull final LinkedBlockingQueue<WrappersObserver> wrappersQueue) {
            this.observer = observer;
            this.wrappersQueue = wrappersQueue;
        }

        @Override
        public void run() {
            try {
                final WrappersObserver wrappersObserver = wrappersQueue.poll(3, TimeUnit.SECONDS);
                observer.removeObserver(wrappersObserver);
                System.out.println("remove: " + wrappersObserver.hashCode());
                lock.countDown();
            } catch (final InterruptedException e) {
                fail();
            }
        }
    }


    private class SimpleWrappersObserver implements WrappersObserver {

        @Override
        public void notifyDatabaseListRetrieved(@NonNull final DatabaseList databaseList) {

        }

        @Override
        public void notifySnapshotRetrieved(@NonNull final Snapshot snapshot,
                                            final long databaseRevision) {

        }

        @Override
        public void notifyCollectionReseted(@NonNull final YDSContext databaseContext,
                                            @NonNull final String databaseId,
                                            @NonNull final String collectionId) {

        }

        @Override
        public void notifyDatabaseReseted(@NonNull final YDSContext databaseContext,
                                          @NonNull final String databaseId) {

        }

        @Override
        public void notifyDatabaseSynced(@NonNull final Database database) {

        }

        @Override
        public void notifyError(@NonNull final Error error) {

        }

        @Override
        public void notifyCollectionRetrieved(@NonNull final Collection collection,
                                              final long databaseRevision) {

        }

        @Override
        public void notifyFullSyncSuccess(@NonNull final YDSContext databaseContext) {

        }

        @Override
        public void notifyFullSyncFailed(@NonNull final YDSContext databaseContext) {

        }

        @Override
        public void notifyDatabaseInfoRetrieved(@NonNull final Database databaseWrapper) {

        }

        @Override
        public void notifyDatabaseCreated(@NonNull final Database databaseWrapper) {

        }
    }
}