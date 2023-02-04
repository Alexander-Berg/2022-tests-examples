package ru.yandex.partner.test.db;


import java.util.List;

import org.jooq.ExecuteContext;
import org.jooq.conf.ParamType;
import org.jooq.impl.DefaultExecuteListener;

public class QueryListener extends DefaultExecuteListener {

    private final List<String> queryLog;
    private boolean active;

    public QueryListener(List<String> queryLog) {
        this.queryLog = queryLog;
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void executeStart(ExecuteContext ctx) {
        if (active) {
            String queryWithComment = ctx.query().getSQL(ParamType.INLINED).concat(";\n");
            String query = queryWithComment.substring(queryWithComment.indexOf("*/") + 2);
            String comment = queryWithComment.substring(0, queryWithComment.indexOf("*/") + 2);

            if (query.startsWith("select ")) {
                int indexOfFrom = query.indexOf("from");
                query = query.substring(0, indexOfFrom)
                        .replaceFirst("select ", "select\n  ")
                        .replace(", ", ",\n  ")
                        .concat(query.substring(indexOfFrom));
            } else if (query.startsWith("insert into") && query.contains("values")) {
                int indexOfValues = query.indexOf("values");
                String substr = query.substring(0, indexOfValues);
                if (!substr.substring(0, indexOfValues - 1).contains("\n")) {
                    query = substr
                            .replaceFirst("\\(", "\\(\n  ")
                            .replaceFirst("\\)", "\n\\)")
                            .replace(", ", ",\n  ")
                            .concat(query.substring(indexOfValues));
                }
            }

            queryLog.add(comment + "\n" + query);
        }
        super.executeStart(ctx);
    }

    public List<String> getQueryLog() {
        return queryLog;
    }

    public void clearQueryLog() {
        queryLog.clear();
    }
}
