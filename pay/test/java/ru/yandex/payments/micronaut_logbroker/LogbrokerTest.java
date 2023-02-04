package ru.yandex.payments.micronaut_logbroker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.yandex.ydb.persqueue.PersqueueErrorCodes.EErrorCode;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.grpc.annotation.GrpcChannel;
import io.micronaut.retry.annotation.RetryPredicate;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import ru.yandex.kikimr.persqueue.producer.ProducerException;
import ru.yandex.kikimr.persqueue.producer.ProducerStreamClosedException;
import ru.yandex.payments.micronaut_logbroker.annotations.LogbrokerListener;
import ru.yandex.payments.micronaut_logbroker.annotations.LogbrokerProducer;
import ru.yandex.payments.micronaut_logbroker.annotations.OffsetStrategy;
import ru.yandex.payments.micronaut_logbroker.annotations.Topic;
import ru.yandex.payments.micronaut_logbroker.consumer.Committer;
import ru.yandex.payments.micronaut_logbroker.consumer.ConsumerMessage;
import ru.yandex.payments.micronaut_logbroker.consumer.ConsumerRecord;
import ru.yandex.payments.micronaut_logbroker.consumer.LogbrokerListenerExceptionHandler;
import ru.yandex.payments.micronaut_logbroker.exceptions.LogbrokerListenerException;
import ru.yandex.payments.micronaut_logbroker.manager.ManagerClient;
import ru.yandex.payments.micronaut_logbroker.manager.ManagerClientFactory;
import ru.yandex.payments.micronaut_logbroker.manager.dto.Codec;
import ru.yandex.payments.micronaut_logbroker.manager.dto.LimitsMode;
import ru.yandex.payments.micronaut_logbroker.manager.dto.Permission;
import ru.yandex.payments.micronaut_logbroker.manager.dto.PermissionSubject;
import ru.yandex.payments.micronaut_logbroker.producer.Producer;
import ru.yandex.payments.micronaut_logbroker.producer.RecordMetadata;

import static java.util.Collections.synchronizedList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.waitAtMost;
import static ru.yandex.payments.micronaut_logbroker.manager.dto.DataPermission.ALLOW;

@Introspected
record MyMessage(String msg,
                 int code) {
}

@Slf4j
@MicronautTest
class LogbrokerTest {
    private static final Duration TEST_TIMEOUT = Duration.ofMinutes(15);

    private static final String TOPIC = "${test-topic}";
    private static final String LB_ASYNC_CONSUMER = "${test-async-consumer}";
    private static final String LB_SYNC_CONSUMER = "${test-sync-consumer}";
    private static final String LB_MANUAL_CONSUMER = "${test-manual-consumer}";

    public static final String SYNC_CONSUMER = "sync";
    public static final String ASYNC_CONSUMER = "async";
    public static final String MANUAL_CONSUMER = "manual";

    static class ConsumerLogicException extends RuntimeException {
        ConsumerLogicException() {
            super("Consumer logic exception");
        }
    }

    public static class PermissionsRetryPredicate implements RetryPredicate {
        @Override
        public boolean test(Throwable throwable) {
            return throwable instanceof ProducerStreamClosedException &&
                    throwable.getCause() instanceof StatusRuntimeException &&
                    throwable.getCause().getCause() instanceof ProducerException producerEx &&
                    producerEx.getErrorCode() == EErrorCode.ACCESS_DENIED;
        }
    }

    @Singleton
    static class Retryer {
        @Retryable(delay = "5s")
        void retryLogbrokerRelease(Runnable release) {
            release.run();
        }

        @Retryable(attempts = "9999", delay = "20s", maxDelay = "10m", predicate = PermissionsRetryPredicate.class)
        Mono<RecordMetadata> retryWrite(Supplier<Mono<RecordMetadata>> writeSupplier) {
            return Mono.defer(writeSupplier);
        }
    }

    @Singleton
    static class ConsumerStorage {
        @Getter
        private final Map<String, List<ConsumerRecord<MyMessage>>> messages = new ConcurrentHashMap<>();
        @Getter
        private final Map<String, List<LogbrokerListenerException>> errors = new ConcurrentHashMap<>();
        private final AtomicInteger messagesCount = new AtomicInteger(0);

        void putMessage(String consumer, ConsumerRecord<MyMessage> message) {
            messages.computeIfAbsent(consumer, ignored -> synchronizedList(new ArrayList<>()))
                    .add(message);
            messagesCount.incrementAndGet();
        }

        void putError(String consumer, LogbrokerListenerException exception) {
            // caused by access denied at the beginning of the test
            if (exception.getCause() instanceof StatusRuntimeException) {
                return;
            }

            errors.computeIfAbsent(consumer, ignored -> synchronizedList(new ArrayList<>()))
                    .add(exception);
        }

        int getTotalMessagesCount() {
            return messagesCount.get();
        }
    }

    @LogbrokerListener(consumerId = ASYNC_CONSUMER, strategy = OffsetStrategy.ASYNC)
    static class AsyncConsumer implements LogbrokerListenerExceptionHandler {
        private final ConsumerStorage storage;
        private boolean failed;

        @Inject
        public AsyncConsumer(ConsumerStorage storage) {
            this.storage = storage;
            this.failed = false;
        }

        @Topic(name = TOPIC)
        public Mono<Void> consume(ConsumerRecord<MyMessage> record) {
            if (failed) {
                storage.putMessage(ASYNC_CONSUMER, record);
                return Mono.empty();
            } else {
                failed = true;
                return Mono.error(new ConsumerLogicException());
            }
        }

        @Override
        public void handle(LogbrokerListenerException exception) {
            storage.putError(ASYNC_CONSUMER, exception);
        }
    }

    @Topic(name = TOPIC)
    @LogbrokerListener(consumerId = SYNC_CONSUMER, strategy = OffsetStrategy.SYNC)
    static class SyncConsumer implements LogbrokerListenerExceptionHandler {
        private final ConsumerStorage storage;
        private boolean failed;

        @Inject
        public SyncConsumer(ConsumerStorage storage) {
            this.storage = storage;
            this.failed = false;
        }

        public Mono<Void> consume(ConsumerRecord<MyMessage> record) {
            if (failed) {
                storage.putMessage(SYNC_CONSUMER, record);
                return Mono.empty();
            } else {
                failed = true;
                return Mono.error(new ConsumerLogicException());
            }
        }

        @Override
        public void handle(LogbrokerListenerException exception) {
            storage.putError(SYNC_CONSUMER, exception);
        }
    }

    @LogbrokerListener(consumerId = MANUAL_CONSUMER, strategy = OffsetStrategy.DISABLED)
    static class ManualConsumer implements LogbrokerListenerExceptionHandler {
        private final ConsumerStorage storage;
        private boolean failed;

        @Inject
        public ManualConsumer(ConsumerStorage storage) {
            this.storage = storage;
            this.failed = false;
        }

        @Topic(name = TOPIC)
        public void consume(ConsumerRecord<MyMessage> record, Committer committer) {
            if (failed) {
                committer.commitSync();
                storage.putMessage(MANUAL_CONSUMER, record);
            } else {
                failed = true;
                throw new ConsumerLogicException();
            }
        }

        @Override
        public void handle(LogbrokerListenerException exception) {
            storage.putError(MANUAL_CONSUMER, exception);
        }
    }

    @Value(TOPIC)
    String topicName;

    @Value(LB_SYNC_CONSUMER)
    String syncConsumer;

    @Value(LB_ASYNC_CONSUMER)
    String asyncConsumer;

    @Value(LB_MANUAL_CONSUMER)
    String manualConsumer;

    @Inject
    ManagerClientFactory managerClientFactory;

    @Inject
    @GrpcChannel("manager")
    ManagedChannel managerChannel;

    @Inject
    @LogbrokerProducer("main")
    Producer<MyMessage> producer;

    @Inject
    ConsumerStorage storage;

    @Inject
    Retryer retryer;

    @Value("${user-login}")
    String login;

    private static String restoreTopicName(String name) {
        return name.substring(name.indexOf("--")) // cut DC name
                .replace("--", "/");
    }

    void initLogbroker(ManagerClient manager) {
        val codecs = EnumSet.allOf(Codec.class);
        val abc = Optional.<String>empty();
        val responsible = Optional.<String>empty();
        val permission = new Permission(PermissionSubject.staff(login), ALLOW, ALLOW, ALLOW);

        val requests = List.of(
                manager.createTopic(topicName, 1, Duration.ofMinutes(10), abc, responsible, codecs),
                manager.createConsumer(syncConsumer, false, LimitsMode.WAIT, codecs, abc, responsible),
                manager.createConsumer(asyncConsumer, false, LimitsMode.WAIT, codecs, abc, responsible),
                manager.createConsumer(manualConsumer, false, LimitsMode.WAIT, codecs, abc, responsible),
                manager.createReadRule(topicName, syncConsumer, false, Optional.empty()),
                manager.createReadRule(topicName, asyncConsumer, false, Optional.empty()),
                manager.createReadRule(topicName, manualConsumer, false, Optional.empty()),
                manager.grantTopicPermissions(topicName, List.of(permission))
        );
        manager.execute(requests).block(TEST_TIMEOUT);
    }

    void releaseLogbroker(ManagerClient manager) {
        val requests = List.of(
                manager.deleteReadRule(topicName, syncConsumer, Optional.empty()),
                manager.deleteReadRule(topicName, asyncConsumer, Optional.empty()),
                manager.deleteReadRule(topicName, manualConsumer, Optional.empty()),
                manager.deleteConsumer(syncConsumer),
                manager.deleteConsumer(asyncConsumer),
                manager.deleteConsumer(manualConsumer),
                manager.deleteTopic(topicName)
        );

        manager.execute(requests).block(TEST_TIMEOUT);
    }

    private Mono<RecordMetadata> write(MyMessage message) {
        return retryer.retryWrite(() -> producer.write(message));
    }

    @Test
    @DisplayName("Verify that @LogbrokerListener could read all the data @LogbrokerProducer write")
    void testProducerConsumerScheme() {
        val expectedTotalMessagesCount = 6; // 2 per consumer

        val manager = managerClientFactory.getClient(managerChannel);
        initLogbroker(manager);

        val message1 = new MyMessage("message", 0);
        val message2 = new MyMessage("msg2", 1);

        try {
            write(message1).block(TEST_TIMEOUT);
            write(message2).block(TEST_TIMEOUT);

            waitAtMost(TEST_TIMEOUT)
                    .until(() -> storage.getTotalMessagesCount() >= expectedTotalMessagesCount);
        } finally {
            try {
                retryer.retryLogbrokerRelease(() -> releaseLogbroker(manager));
            } catch (Exception e) {
                log.error("Error releasing logbroker stuff", e);
            }
        }

        assertThat(storage.getMessages())
                .describedAs("Verify how much consumers receive a message")
                .hasSize(3);
        assertThat(storage.getTotalMessagesCount())
                .describedAs("Verify total received messages count")
                .isEqualTo(expectedTotalMessagesCount);
        assertThat(storage.getMessages().keySet())
                .containsExactlyInAnyOrder(ASYNC_CONSUMER, SYNC_CONSUMER, MANUAL_CONSUMER);

        storage.getMessages().forEach((consumer, records) -> {
            val messages = StreamEx.of(records)
                    .flatCollection(ConsumerRecord::messages)
                    .toImmutableList();

            assertThat(messages)
                    .describedAs("Consumer [%s] messages", consumer)
                    .allSatisfy(msg -> {
                        assertThat(msg)
                                .extracting(ConsumerMessage::topic)
                                .extracting(LogbrokerTest::restoreTopicName)
                                .isEqualTo(topicName);
                        assertThat(msg.meta().getExtraFields())
                                .contains(entry("custom-key", List.of("42")));
                    })
                    .extracting(ConsumerMessage::value)
                    .containsExactly(message1, message2);
        });

        assertThat(storage.getErrors())
                .describedAs("Verify how much consumers throws an error")
                .hasSize(3);
        assertThat(storage.getErrors().keySet())
                .containsExactlyInAnyOrder(ASYNC_CONSUMER, SYNC_CONSUMER, MANUAL_CONSUMER);

        val exceptions = EntryStream.of(storage.getErrors())
                .flatMapValues(Collection::stream)
                .values()
                .map(Throwable::getCause)
                .toImmutableSet();
        assertThat(exceptions)
                .allMatch(ConsumerLogicException.class::isInstance)
                .hasSize(3);
    }
}
