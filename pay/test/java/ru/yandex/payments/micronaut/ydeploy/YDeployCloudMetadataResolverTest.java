package ru.yandex.payments.micronaut.ydeploy;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.netty.channel.ChannelPipelineCustomizer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.util.cloud.DC;
import ru.yandex.payments.util.cloud.YaCloudEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(environments = YaCloudEnvironment.Y_DEPLOY)
class YDeployCloudMetadataResolverTest {
    @Inject
    YDeployCloudMetadataResolver resolver;

    @Controller
    static class PodAgentStub {
        @Get("/pod_attributes")
        HttpResponse<String> podAttributes() {
            return HttpResponse.ok("""
                    {"node_meta": {"dc": "vla"}, "metadata": {"pod_id": "id"}}""");
        }
    }

    private static class ContentTypeErasureHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof io.netty.handler.codec.http.HttpResponse response) {
                response.headers().remove("Content-Type");
            }

            super.write(ctx, msg, promise);
        }
    }

    @Singleton
    static class ContentTypeEraser implements BeanCreatedEventListener<ChannelPipelineCustomizer> {
        @Override
        public ChannelPipelineCustomizer onCreated(BeanCreatedEvent<ChannelPipelineCustomizer> event) {
            val customizer = event.getBean();

            if (customizer.isServerChannel()) {
                customizer.doOnConnect(pipeline -> {
                    pipeline.addAfter(
                            ChannelPipelineCustomizer.HANDLER_HTTP_SERVER_CODEC,
                            "content-type-eraser",
                            new ContentTypeErasureHandler()
                    );
                    return pipeline;
                });
            }

            return customizer;
        }
    }

    @Test
    @DisplayName("Verify that YDeployCloudMetadataResolver returns correct metadata")
    void test() {
        val metadata = resolver.resolveMetadata().block();
        assertThat(metadata)
                .isNotNull();

        assertThat(metadata.cloudName())
                .isEqualTo(YDeployCloudMetadata.CLOUD_NAME);
        assertThat(metadata.dc())
                .isEqualTo(DC.VLA);
        assertThat(metadata.instanceId())
                .contains("id");
    }
}
