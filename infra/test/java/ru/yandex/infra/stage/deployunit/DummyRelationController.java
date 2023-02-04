package ru.yandex.infra.stage.deployunit;

import java.util.concurrent.CompletableFuture;

import ru.yandex.infra.stage.yp.RelationController;

public class DummyRelationController implements RelationController {

    public String lastUsedStageFqid;

    @Override
    public CompletableFuture<?> addRelation(String stageFqid, String childFqid) {
        return CompletableFuture.completedFuture(0);
    }

    @Override
    public CompletableFuture<?> removeRelation(String stageFqid, String childFqid) {
        return CompletableFuture.completedFuture(0);
    }

    @Override
    public boolean containsRelation(String stageFqid, String childFqid) {
        lastUsedStageFqid = stageFqid;
        return true;
    }
}
