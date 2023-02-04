package ru.yandex.infra.auth.yp;

import ru.yandex.infra.controller.dto.SchemaMeta;

public class DummyYpObjectTreeGetter implements YpObjectsTreeGetter {
    public TreeNodeWithTimestamp root = new TreeNodeWithTimestamp(null, 0L, null);

    public TreeNodeWithTimestamp getObjectsTree(boolean withMetrics) {
        return root;
    }

    @Override
    public String getUniqueIdForProjectOrStageIdmNode(SchemaMeta meta) {
        return meta.getUuid();
    }
}
