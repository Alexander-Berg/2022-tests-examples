package ru.yandex.solomon.alert.notification.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.notification.domain.email.CloudEmailNotification;
import ru.yandex.solomon.alert.notification.domain.email.DatalensEmailNotification;
import ru.yandex.solomon.alert.notification.domain.email.EmailNotification;
import ru.yandex.solomon.alert.notification.domain.juggler.JugglerNotification;
import ru.yandex.solomon.alert.notification.domain.push.CloudPushNotification;
import ru.yandex.solomon.alert.notification.domain.sms.CloudSmsNotification;
import ru.yandex.solomon.alert.notification.domain.sms.SmsNotification;
import ru.yandex.solomon.alert.notification.domain.telegram.TelegramNotification;
import ru.yandex.solomon.alert.notification.domain.webhook.WebhookNotification;
import ru.yandex.solomon.alert.notification.domain.yachats.YaChatsNotification;
import ru.yandex.solomon.alert.protobuf.Severity;

/**
 * @author Vladimir Gordiychuk
 */
public class NotificationTestSupport {
    public static Notification randomNotification() {
        return randomNotification(ThreadLocalRandom.current());
    }

    public static Notification randomNotification(ThreadLocalRandom random) {
        List<NotificationType> types = Arrays.stream(NotificationType.values())
                .filter(type -> !type.equals(NotificationType.UNKNOWN))
                .collect(Collectors.toList());
        NotificationType type = types.get(random.nextInt(types.size()));
        return randomNotification(random, type);
    }

    public static Notification randomNotification(NotificationType type) {
        return randomNotification(ThreadLocalRandom.current(), type);
    }

    public static Notification randomNotification(ThreadLocalRandom random, NotificationType type) {
        return switch (type) {
            case EMAIL -> randomEmailNotification(random);
            case JUGGLER -> randomJugglerNotification(random);
            case WEBHOOK -> randomWebhookNotification(random);
            case SMS -> randomSmsNotification(random);
            case TELEGRAM -> randomTelegramNotification(random);
            case CLOUD_EMAIL -> randomCloudEmailNotification(random);
            case CLOUD_SMS -> randomCloudSmsNotification(random);
            case YA_CHATS -> randomYaChatsNotification(random);
            case DATALENS_EMAIL -> randomDatalensEmailNotification(random);
            case CLOUD_PUSH -> randomCloudPushNotification(random);
            case PHONE_CALL -> randomPhoneNotification(random);
            default -> throw new NotImplementedException("Implement randomNotification for " + type);
        };
    }

    public static PhoneNotification randomPhoneNotification(ThreadLocalRandom random) {
        var builder = PhoneNotification.newBuilder();
        if (random.nextBoolean()) {
            builder
                    .setLogin(UUID.randomUUID().toString())
                    .setDuty(PhoneNotification.AbcDuty.EMPTY);
        } else {
            builder
                    .setLogin("")
                    .setDuty(new PhoneNotification.AbcDuty(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        }

        Set<EvaluationStatus.Code> notifyAbout = EnumSet.allOf(EvaluationStatus.Code.class);
        notifyAbout.removeIf(status -> random.nextBoolean());

        String id = UUID.randomUUID().toString();
        return builder
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Name with random: " + random.nextInt())
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setNotifyAboutStatus(notifyAbout)
                .setRepeatNotifyDelay(Duration.ofMillis(random.nextLong(-1, TimeUnit.DAYS.toMillis(1L))))
                .build();
    }

    public static WebhookNotification randomWebhookNotification(ThreadLocalRandom random) {
        final String tempate;
        if (random.nextBoolean()) {
            tempate = "{\"alertId\": \"{{alert.id}}\", \"status\": \"{{status.code}}\", \"when\": \"{{since}}\"}";
        } else {
            tempate = "{\"alertId\": \"{{alert.id}}\"}";
        }

        Set<EvaluationStatus.Code> notifyAbout = EnumSet.allOf(EvaluationStatus.Code.class);
        notifyAbout.removeIf(status -> random.nextBoolean());

        String id = UUID.randomUUID().toString();
        return WebhookNotification.newBuilder()
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setUrl("http://localhost:8181/alert/" + random.nextInt())
                .setTemplate(tempate)
                .setName("Name with random: " + random.nextInt())
                .setHeaders(ImmutableMap.of("Expect", String.valueOf(random.nextInt())))
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setNotifyAboutStatus(notifyAbout)
                .setRepeatNotifyDelay(Duration.ofMillis(random.nextLong(-1, TimeUnit.DAYS.toMillis(1L))))
                .build();
    }

    public static EmailNotification randomEmailNotification(ThreadLocalRandom random) {
        Set<String> recipients = Sets.newHashSet(
                "valarie.coffey@yandex-team.ru",
                "frank.osborne@yandex-team.ru",
                "mary.phillis@gmail.com",
                "karin.ruiz@yandex.ru");

        recipients.removeIf(s -> random.nextBoolean());
        if (recipients.isEmpty()) {
            recipients.add("solomon-test@yandex-team.ru");
        }

        Set<EvaluationStatus.Code> notifyAbout = EnumSet.allOf(EvaluationStatus.Code.class);
        notifyAbout.removeIf(status -> random.nextBoolean());

        String id = UUID.randomUUID().toString();
        return EmailNotification.newBuilder()
                .setId(id)
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description for email channel with other random: " + random.nextInt())
                .setRecipients(recipients)
                .setSubjectTemplate(random.nextBoolean() ? "[{{status.code}}]: {{alert.id}}" : "{{alert.name}}")
                .setContentTemplate(random.nextBoolean() ? "Everything is broken" : "{{status.code}}")
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setNotifyAboutStatus(notifyAbout)
                .setRepeatNotifyDelay(Duration.ofMillis(random.nextLong(-1, TimeUnit.DAYS.toMillis(1L))))
                .setLabels(Map.of("label1", "value1"))
                .build();
    }

    public static JugglerNotification randomJugglerNotification(ThreadLocalRandom random) {
        List<String> hosts = Arrays.asList("solomon-stp-man", "solomon-stp-myt", "", "solomon-alert-{{alert.id}}");
        List<String> services = Arrays.asList("my-alert-check", "", "{{alert.id}}");
        List<String> descriptions = Arrays.asList("", "[{{status.code}}]: {{alert.id}}", "{{alert.name}}", "все пропало!");

        Set<String> tags = Sets.newHashSet(
                "solomon-alert",
                "{{alert.id}}",
                UUID.randomUUID().toString(),
                getHostName());
        tags.removeIf(s -> random.nextBoolean());

        Set<EvaluationStatus.Code> notifyAbout = EnumSet.allOf(EvaluationStatus.Code.class);
        notifyAbout.removeIf(status -> random.nextBoolean());

        String id = UUID.randomUUID().toString();
        return JugglerNotification.newBuilder()
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description for juggler channel with other random: " + random.nextInt())
                .setHost(hosts.get(random.nextInt(hosts.size())))
                .setService(services.get(random.nextInt(services.size())))
                .setInstance(random.nextBoolean() ? getHostName() : "")
                .setJugglerDescription(descriptions.get(random.nextInt(descriptions.size())))
                .setTags(tags)
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setNotifyAboutStatus(notifyAbout)
                .setLabels(Map.of("label1", "value2"))
                .build();
    }

    public static SmsNotification randomSmsNotification(ThreadLocalRandom random) {
        Set<EvaluationStatus.Code> notifyAbout = EnumSet.allOf(EvaluationStatus.Code.class);
        notifyAbout.removeIf(status -> random.nextBoolean());

        String id = UUID.randomUUID().toString();
        SmsNotification.Builder builder = SmsNotification.newBuilder()
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description for sms channel with other random: " + random.nextInt())
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setLabels(Map.of("label1", "value3"))
                .setNotifyAboutStatus(notifyAbout);

        if (random.nextBoolean()) {
            builder.setPhone("+71234567890");
        } else {
            builder.setLogin("alice");
        }

        return builder.build();
    }

    public static TelegramNotification randomTelegramNotification(ThreadLocalRandom random) {
        Set<EvaluationStatus.Code> notifyAbout = EnumSet.allOf(EvaluationStatus.Code.class);
        notifyAbout.removeIf(status -> random.nextBoolean());

        String id = UUID.randomUUID().toString();
        String template = null;
        if (random.nextBoolean()) {
            template = UUID.randomUUID().toString();
        }

        TelegramNotification.Builder builder = TelegramNotification.newBuilder()
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description for telegram channel with other random: " + random.nextInt())
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setNotifyAboutStatus(notifyAbout)
                .setLabels(Map.of("label1", "value4"))
                .setTextTemplate(template);

        if (random.nextBoolean()) {
            builder.setLogin("alexlovkov");
        }
        if (random.nextBoolean()) {
            builder.setSendScreenshot(random.nextBoolean());
        }
        return builder.build();
    }

    public static YaChatsNotification randomYaChatsNotification(ThreadLocalRandom random) {
        Set<EvaluationStatus.Code> notifyAbout = EnumSet.allOf(EvaluationStatus.Code.class);
        notifyAbout.removeIf(status -> random.nextBoolean());

        String id = UUID.randomUUID().toString();
        String template = null;
        if (random.nextBoolean()) {
            template = UUID.randomUUID().toString();
        }

        YaChatsNotification.Builder builder = YaChatsNotification.newBuilder()
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description for ya chats channel with other random: " + random.nextInt())
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setNotifyAboutStatus(notifyAbout)
                .setLabels(Map.of("label1", "value5"))
                .setTextTemplate(template);

        if (random.nextBoolean()) {
            builder.setLogin("alexlovkov");
        } else {
            builder.setGroupId("0/0/73c3c30b-570b-4ff3-ae29-0994f771ee3f");
        }
        return builder.build();
    }

    private static CloudEmailNotification randomCloudEmailNotification(ThreadLocalRandom random) {
        String id = UUID.randomUUID().toString();
        return CloudEmailNotification.newBuilder()
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description for cloud email channel with other random: " + random.nextInt())
                .setRecipients(Set.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setLabels(Map.of("label1", "value6"))
                .build();
    }

    private static Notification randomDatalensEmailNotification(ThreadLocalRandom random) {
        String id = UUID.randomUUID().toString();
        return DatalensEmailNotification.newBuilder()
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description for datalens email channel with other random: " + random.nextInt())
                .setRecipients(Set.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setLabels(Map.of("label1", "value7"))
                .build();
    }

    private static CloudSmsNotification randomCloudSmsNotification(ThreadLocalRandom random) {
        String id = UUID.randomUUID().toString();
        return CloudSmsNotification.newBuilder()
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description for cloud sms channel with other random: " + random.nextInt())
                .setRecipients(Set.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setLabels(Map.of("label1", "value8"))
                .build();
    }

    private static CloudPushNotification randomCloudPushNotification(ThreadLocalRandom random) {
        String id = UUID.randomUUID().toString();
        return CloudPushNotification.newBuilder()
                .setId(id)
                .setDefaultForProject(ThreadLocalRandom.current().nextBoolean())
                .setDefaultForSeverity(Set.of(AlertSeverity.forNumber(ThreadLocalRandom.current().nextInt(0, Severity.values().length - 1))))
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Name with random: " + random.nextInt())
                .setDescription("Description for cloud push channel with other random: " + random.nextInt())
                .setRecipients(Set.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .setUpdatedBy(randomUser(random))
                .setUpdatedAt(Instant.now().plusMillis(random.nextLong(0, 1_000_000)))
                .setCreatedBy(randomUser(random))
                .setCreatedAt(Instant.now().minusMillis(random.nextLong(0, 1_000_000)))
                .setVersion(random.nextInt(0, 1000))
                .setLabels(Map.of("label1", "value9"))
                .build();
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }


    private static String randomUser(ThreadLocalRandom random) {
        switch (random.nextInt(5)) {
            case 0:
                return "";
            case 1:
                return "Justin Wiley";
            case 2:
                return "Teresa Monti";
            default:
                return RandomStringUtils.randomAlphanumeric(10);
        }
    }
}
