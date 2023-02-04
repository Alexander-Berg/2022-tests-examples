package ru.yandex.infra.stage.deployunit;

public class DummySandboxResourcesResolveResultHandler implements SandboxResourcesResolveResultHandler {
    @Override
    public void onSandboxResourceResolveSuccess() {
    }

    @Override
    public void onSandboxResourceResolveFailure(String resourceId, String sbrId, Throwable ex) {
    }
}
