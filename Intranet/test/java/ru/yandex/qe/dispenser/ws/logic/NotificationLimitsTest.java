package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import org.apache.commons.collections4.IterableUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.request.DiResourceAmount;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.aspect.HierarchyRequiredAspect;
import ru.yandex.qe.dispenser.domain.dao.notifications.NotificationsDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.property.PropertyDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.domain.notifications.NotificationEntry;
import ru.yandex.qe.dispenser.domain.notifications.NotificationManager;
import ru.yandex.qe.dispenser.standalone.MockEmailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotificationLimitsTest extends BusinessLogicTestBase {
    @Autowired(required = false)
    NotificationManager notificationManager;

    private final MockEmailSender emailSender = new MockEmailSender();

    @Autowired(required = false)
    private NotificationsDao notificationsDao;

    @Autowired
    private PropertyDao propertyDao;

    @Autowired
    private PersonDao personDao;

    @BeforeAll
    public void configure() {
        Assumptions.assumeFalse(notificationManager == null, "NotificationManager is not initialized");
        notificationManager.setIsEnabled(true);
        notificationManager.setNotificationFilteringPredicate(NotificationManager.parseNotificationFilteringPredicate(String.join(",", NIRVANA, MDS)));
        notificationManager.setEmailSender(emailSender);
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        emailSender.clear();
        propertyDao.clear();
        notificationsDao.clear();
    }

    private void sendNotifications() {
        updateHierarchy();
        HierarchyRequiredAspect.runWithDisabledCheck(() -> notificationManager.sendNotifications());
    }

    private void assertMessagesContainTexts(final Collection<SimpleMailMessage> mailMessages, final String... messages) {
        for (final String message : messages) {
            assertTrue(mailMessages.stream().anyMatch(mm -> mm.getText().contains(message)),
                    "Expected message: '" + message + "', actual messages: '" + mailMessages + "'");
        }
    }

    private void syncOwnMax(final String service, final String project, final String resource, final DiAmount amount) {
        dispenser()
                .service(service)
                .syncState()
                .quotas()
                .changeQuota(
                        DiQuotaState
                                .forResource(resource)
                                .forProject(project)
                                .withOwnMax(amount)
                                .build()
                )
                .performBy(AGODIN);
        updateHierarchy();
    }

    private void syncActual(final String service, final String project, final String resource, final DiAmount amount) {
        dispenser()
                .service(service)
                .syncState()
                .quotas()
                .changeQuota(
                        DiQuotaState
                                .forResource(resource)
                                .forProject(project)
                                .withActual(amount)
                                .build()
                )
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        updateHierarchy();
    }


    private void createLimitsFor(final String service, final Integer... limits) {
        dispenser()
                .properties()
                .setProperty(NotificationManager.QUOTA_LIMITS_PROPERTY, service, Joiner.on(',').join(limits))
                .performBy(AMOSOV_F);
        updateHierarchy();
    }

    private DiEntity createNirvanaYtFileOfSize(@NotNull final DiAmount size) {
        final DiEntity entity = DiEntity.withKey("yt-file-" + System.currentTimeMillis())
                .bySpecification(YT_FILE)
                .occupies(DiResourceAmount.ofResource(STORAGE).withAmount(size).build())
                .build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(entity, LYADZHIN.chooses(INFRA))
                .perform();
        return entity;
    }

    @Test
    public void notificationShouldBeSentWithCustomLimit() {
        createLimitsFor(NIRVANA, 50);

        createNirvanaYtFileOfSize(DiAmount.of(50, DiUnit.BYTE));

        sendNotifications();

        List<SimpleMailMessage> messages = emailSender.getMessages();
        assertEquals(1, messages.size());

        final SimpleMailMessage first = IterableUtils.first(messages);
        assertEquals(1, notificationsDao.getActualNotifications().size());
        assertTrue(first.getText().contains("превысило лимит в 50 %"));
        assertEquals("[Nirvana] Infrastruktura: превышен лимит потребления квоты в 50 %", first.getSubject());

        createNirvanaYtFileOfSize(DiAmount.of(50, DiUnit.BYTE));

        sendNotifications();

        messages = emailSender.getMessages();
        assertEquals(2, messages.size());
        assertEquals(2, notificationsDao.getActualNotifications().size());

    }

    @Test
    public void notificationMessageAboutOverquotingForNirvanaShouldBeCorrect() {
        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));

        sendNotifications();

        final List<SimpleMailMessage> messages = emailSender.getMessages();
        assertEquals(1, messages.size());
        assertMessagesContainTexts(messages, "и операции этого проекта могут не запускаться", "превысило лимит в 100 %");
        assertEquals("[Nirvana] Infrastruktura: исчерпание квоты", IterableUtils.first(messages).getSubject());
    }

    @Test
    public void notificationShouldNotRepeatWhenActualExceedLimit() {
        createLimitsFor(NIRVANA, 50, 60);
        createNirvanaYtFileOfSize(DiAmount.of(40, DiUnit.BYTE));
        Set<NotificationEntry> actualNotifications = notificationsDao.getActualNotifications();

        assertEquals(0, actualNotifications.size());
        sendNotifications();

        final List<SimpleMailMessage> messages = emailSender.getMessages();
        assertTrue(messages.isEmpty());
        assertEquals(0, actualNotifications.size());

        createNirvanaYtFileOfSize(DiAmount.of(10, DiUnit.BYTE));

        sendNotifications();
        assertEquals(1, messages.size());
        actualNotifications = notificationsDao.getActualNotifications();
        assertEquals(1, actualNotifications.size());


        final DiEntity file = createNirvanaYtFileOfSize(DiAmount.of(10, DiUnit.BYTE));

        sendNotifications();
        assertEquals(2, messages.size());
        actualNotifications = notificationsDao.getActualNotifications();
        assertEquals(2, actualNotifications.size());


        dispenser().quotas().changeInService(NIRVANA).releaseEntity(file).perform();

        sendNotifications();
        assertEquals(2, messages.size());
        actualNotifications = notificationsDao.getActualNotifications();
        assertEquals(1, actualNotifications.size());
    }

    @Test
    public void notificationShouldBeSentOnceIfActualJumpOverSeveralLimits() {
        createLimitsFor(NIRVANA, 50, 60);

        createNirvanaYtFileOfSize(DiAmount.of(60, DiUnit.BYTE));

        assertTrue(emailSender.getMessages().isEmpty());

        sendNotifications();

        final List<SimpleMailMessage> messages = emailSender.getMessages();
        assertEquals(1, messages.size());
        assertEquals(2, notificationsDao.getActualNotifications().size());

        final String template = "Вы получили это письмо, так как являетесь ответственным за проект"
                + " \"Infrastruktura\" в Dispenser (https://dispenser-dev.yandex-team.ru/projects/infra).\n"
                + "Потребление по квоте \"Nirvana storage quota.\" в сервисе \"Nirvana\" для проекта \"Infrastruktura\" превысило лимит в %d %%";
        assertMessagesContainTexts(messages, String.format(template, 60));
    }

    @Test
    public void notificationForMDSShouldBeAboutOwnQuota() {
        projectDao.attach(personDao.readPersonByLogin(WHISTLER.getLogin()), projectDao.read(INFRA), Role.RESPONSIBLE);
        updateHierarchy();
        syncOwnMax(MDS, YANDEX, HDD, DiAmount.of(100, DiUnit.BYTE));
        syncOwnMax(MDS, INFRA, HDD, DiAmount.of(100, DiUnit.BYTE));
        createLimitsFor(MDS, 25, 50);

        syncActual(MDS, INFRA, HDD, DiAmount.of(25, DiUnit.BYTE));

        sendNotifications();

        List<SimpleMailMessage> messages = emailSender.getMessages();
        assertEquals(1, messages.size());
        assertEquals(1, notificationsDao.getActualNotifications().size());
        final SimpleMailMessage first = IterableUtils.first(messages);
        assertEquals("[MDS] Infrastruktura: превышен лимит потребления квоты в 25 %", first.getSubject());
        assertMessagesContainTexts(messages, """
                Привет!

                Вы получили это письмо, так как являетесь ответственным за сервис "Infrastruktura" (https://abc.yandex-team.ru/services/infra/folders).
                Потребление по квоте "MDS HDD quota" в провайдере "MDS" для сервиса "Infrastruktura" превысило лимит в 25 %"""
        );
        assertMessagesContainTexts(messages,
                "График потребления: https://solomon.yandex-team.ru/?project=dispenser_common_dev" +
                        "&cluster=dispenser_qloud_env&service=dispenser_dev&graph=auto&l.sensor=infra:%20mds/hdd/hdd" +
                        "&l.attribute=actual%7Cmax&b=31d&stack=0&secondaryGraphMode=none"
        );

        syncActual(MDS, INFRA, HDD, DiAmount.of(50, DiUnit.BYTE));

        sendNotifications();

        messages = emailSender.getMessages();
        assertEquals(2, messages.size());
        assertEquals(2, notificationsDao.getActualNotifications().size());
        assertMessagesContainTexts(messages, "превысило лимит в 25 %", "превысило лимит в 50 %");

        syncActual(MDS, INFRA, HDD, DiAmount.of(100, DiUnit.BYTE));

        sendNotifications();

        messages = emailSender.getMessages();
        assertEquals(3, messages.size());
        assertEquals(3, notificationsDao.getActualNotifications().size());
    }

    @Test
    public void notificationForMDSShouldBeSentOnlyIfActualExceededNonZeroMax() {
        syncOwnMax(MDS, INFRA, HDD, DiAmount.of(0, DiUnit.BYTE));
        createLimitsFor(MDS, 25, 50);

        syncActual(MDS, INFRA, HDD, DiAmount.of(25, DiUnit.BYTE));

        sendNotifications();

        final List<SimpleMailMessage> messages = emailSender.getMessages();
        assertTrue(messages.isEmpty());
    }

    @Test
    public void notificationForMDSShouldBeSentWithLargeActual() {
        projectDao.attach(personDao.readPersonByLogin(WHISTLER.getLogin()), projectDao.read(INFRA), Role.RESPONSIBLE);
        updateHierarchy();
        syncOwnMax(MDS, INFRA, HDD, DiAmount.of(Long.MAX_VALUE - 1, DiUnit.BYTE));
        createLimitsFor(MDS, 25, 50);

        syncActual(MDS, INFRA, HDD, DiAmount.of(Long.MAX_VALUE, DiUnit.BYTE));

        sendNotifications();

        final List<SimpleMailMessage> messages = emailSender.getMessages();
        assertEquals(1, messages.size());
    }
}
