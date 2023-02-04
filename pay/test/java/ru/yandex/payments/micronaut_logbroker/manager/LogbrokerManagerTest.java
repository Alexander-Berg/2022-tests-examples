package ru.yandex.payments.micronaut_logbroker.manager;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import io.grpc.ManagedChannel;
import io.micronaut.grpc.annotation.GrpcChannel;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.micronaut_logbroker.manager.dto.Codec;
import ru.yandex.payments.micronaut_logbroker.manager.dto.LimitsMode;

@MicronautTest
class LogbrokerManagerTest {
    private static final Set<Codec> CODECS = EnumSet.allOf(Codec.class);

    @Inject
    ManagerClientFactory clientFactory;

    @Inject
    @GrpcChannel("test")
    ManagedChannel channel;

    @Test
    void test() {
        val client = clientFactory.getClient(channel);
        val topic = "/payplatform-test/logbroker-manager-test-topic";
        val consumer = "/payplatform-test/logbroker-manager-test-consumer";

        val createRequests = List.of(
                client.createTopic(topic, 1, Duration.ofMinutes(10), Optional.empty(), Optional.empty(), CODECS),
                client.createConsumer(consumer, false, LimitsMode.WAIT, CODECS, Optional.empty(), Optional.empty()),
                client.createReadRule(topic, consumer, false, Optional.empty())
        );

        val removeRequests = List.of(
                client.deleteReadRule(topic, consumer, Optional.empty()),
                client.deleteConsumer(consumer),
                client.deleteTopic(topic)
        );

        client.execute(createRequests).block();
        client.execute(removeRequests).block();
    }
}
