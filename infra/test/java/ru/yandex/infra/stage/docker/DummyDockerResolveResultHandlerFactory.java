package ru.yandex.infra.stage.docker;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import ru.yandex.infra.stage.dto.DockerImageContents;
import ru.yandex.infra.stage.dto.DockerImageDescription;

public class DummyDockerResolveResultHandlerFactory {

    public static DockerResolveResultHandler handler(
            @Nullable Runnable onSuccess,
            @Nullable Runnable onFailure) {
        return new DockerResolveResultHandler() {
            @Override
            public void onDockerResolveSuccess(DockerImageDescription description, DockerImageContents result) {
                if (onSuccess != null)
                    onSuccess.run();
            }

            @Override
            public void onDockerResolveFailure(DockerImageDescription description, Throwable ex) {
                if (onFailure != null)
                    onFailure.run();
            }
        };
    }

    public static DockerResolveResultHandler trueHandler(
            @Nullable BiConsumer<DockerImageDescription, DockerImageContents> onSuccess,
            @Nullable BiConsumer<DockerImageDescription, Throwable> onFailure) {
        return new DockerResolveResultHandler() {
            @Override
            public void onDockerResolveSuccess(DockerImageDescription description, DockerImageContents result) {
                if (onSuccess != null)
                    onSuccess.accept(description, result);
            }

            @Override
            public void onDockerResolveFailure(DockerImageDescription description, Throwable ex) {
                if (onFailure != null)
                    onFailure.accept(description, ex);
            }
        };
    }
}
