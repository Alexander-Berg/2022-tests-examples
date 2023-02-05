package ru.yandex.disk.util;

import rx.Observable;

import javax.annotation.NonnullByDefault;
import java.lang.System;
import java.util.ArrayList;
import java.util.List;

@NonnullByDefault
public class AbstractLogger<E> {

    private final ArrayList<LogEntry> log = new ArrayList<>();

    protected void log(final E e) {
        log.add(new LogEntry(e));
    }

    public ArrayList<LogEntry> getAll() {
        return log;
    }

    public List<? extends Class> getAllClasses() {
        return Observable.from(log)
                .map(logEntry -> logEntry.e.getClass())
                .cast(Class.class)
                .toList()
                .toBlocking().first();
    }

    public <T extends E> T findByClass(final Class<T> eventClass) {
        return observableFindAllByClass(eventClass).defaultIfEmpty(null)
                .toSingle().toBlocking().value();
    }

    public <T extends E> List<T> findAllByClass(final Class<T> eventClass) {
        return observableFindAllByClass(eventClass).toList().toBlocking().first();
    }

    private <T extends E> Observable<T> observableFindAllByClass(final Class<T> eventClass) {
        return Observable.from(log)
                .map(logEntry -> logEntry.e)
                .filter(eventClass::isInstance)
                .cast(eventClass);
    }

    public E get(final int index) {
        if (log.isEmpty()) {
            throw new AssertionError("no events");
        }
        return log.get(index).e;
    }

    public E getFirst() {
        return log.get(0).e;
    }

    public E getLast() {
        return log.get(log.size() - 1).e;
    }

    public int getCount() {
        return log.size();
    }

    public class LogEntry {
        final TraceInfo logHere;
        final E e;

        private LogEntry(final E e) {
            this.e = e;
            logHere = new TraceInfo(String.valueOf(e));
        }

        public E getWrapped() {
            return e;
        }

        @Override
        public String toString() {
            return Exceptions.toString(logHere);
        }
    }

    public void dump() {
        System.out.println(log);
    }
}

