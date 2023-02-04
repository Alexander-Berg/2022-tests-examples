package ru.yandex.partner.test.db;

import java.util.List;
import java.util.function.Supplier;

public class QueryLogService {

    private final QueryListener queryListener;

    public QueryLogService(QueryListener queryListener) {
        this.queryListener = queryListener;
    }

    public List<String> getLog() {
        return queryListener.getQueryLog();
    }

    public void start() {
        queryListener.clearQueryLog();
        queryListener.setActive(true);
    }

    public void stop() {
        queryListener.setActive(false);
    }

    public <T> T executeNoLogQuery(Supplier<T> supplier) {
        T result;
        if (queryListener.isActive()) {
            queryListener.setActive(false);
            result = supplier.get();
            queryListener.setActive(true);
        } else {
            result = supplier.get();
        }

        return result;
    }
}
