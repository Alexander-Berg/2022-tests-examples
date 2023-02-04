package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.request.DiResourceAmount;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.aspect.HierarchyRequiredAspect;
import ru.yandex.qe.dispenser.domain.dao.notifications.EmailSender;
import ru.yandex.qe.dispenser.domain.dao.notifications.NotificationsDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.domain.lots.LotsManager;
import ru.yandex.qe.dispenser.domain.notifications.NotificationEntry;
import ru.yandex.qe.dispenser.domain.notifications.NotificationManager;
import ru.yandex.qe.dispenser.domain.util.CollectionUtils;
import ru.yandex.qe.dispenser.domain.util.DateTimeUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.qe.dispenser.domain.notifications.NotificationManager.parseNotificationFilteringPredicate;

public class NotificationManagerTest extends BusinessLogicTestBase {
    @Autowired(required = false)
    private NotificationManager notificationManager;
    @Autowired(required = false)
    private NotificationsDao notificationsDao;
    @Autowired(required = false)
    private LotsManager lotsManager;
    @Autowired
    private PersonDao personDao;

    @NotNull
    private EmailSender emailSender;

    @BeforeEach
    public void configure() {
        if (notificationManager == null) {
            return;
        }
        emailSender = mock(EmailSender.class);
        notificationManager.setEmailSender(emailSender);
        notificationManager.setNotificationFilteringPredicate(parseNotificationFilteringPredicate(NIRVANA + "/" + STORAGE));
        notificationManager.setIsEnabled(true);
    }

    public void skipIfNeeded() {
        Assumptions.assumeFalse(notificationManager == null || notificationsDao == null, "No notification manager found");
    }

    private void createNirvanaYtFileOfSize(@NotNull final DiAmount size) {
        final DiEntity entity = DiEntity.withKey("yt-file-" + System.currentTimeMillis())
                .bySpecification(YT_FILE)
                .occupies(DiResourceAmount.ofResource(STORAGE).withAmount(size).build())
                .build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(entity, LYADZHIN.chooses(INFRA))
                .perform();
    }

    private void sendNotifications() {
        updateHierarchy();
        HierarchyRequiredAspect.runWithDisabledCheck(() -> notificationManager.sendNotifications());
    }

    @Test
    public void noNotificationsShouldBeSentIfNoOverquotingHappened() {
        skipIfNeeded();

        createNirvanaYtFileOfSize(DiAmount.of(10, DiUnit.BYTE));
        sendNotifications();

        verify(emailSender, never()).sendMessage(anyString(), anyString(), anyCollection());
        assertThat(notificationsDao.getActualNotifications(), is(empty()));
    }

    @Test
    public void properNotificationShouldBeSentIfOverquotingHappened() {
        skipIfNeeded();

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));
        sendNotifications();

        verify(emailSender, only()).sendMessage(anyString(), anyString(), eq(Collections.singleton(WHISTLER.getLogin() + "@yandex-team.ru")));

        final Set<NotificationEntry> actualNotifications = notificationsDao.getActualNotifications();
        assertEquals(1, actualNotifications.size());
        final NotificationEntry notification = CollectionUtils.first(actualNotifications);
        assertEquals(INFRA, notification.getProject().getPublicKey());
        assertEquals(NIRVANA, notification.getSpec().getResource().getService().getKey());
        assertEquals(STORAGE, notification.getSpec().getResource().getKey().getPublicKey());
    }

    @Test
    public void notificationShouldBeSentOnlyOnceAfterEachOverquotingEvent() {
        skipIfNeeded();

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));
        sendNotifications();

        // 1 notification sent, 1 actual notification
        verify(emailSender, only()).sendMessage(anyString(), anyString(), anyCollection());
        assertEquals(1, notificationsDao.getActualNotifications().size());

        createNirvanaYtFileOfSize(DiAmount.of(10, DiUnit.BYTE));
        sendNotifications();

        // no new notifications sent, 1 actual notification
        verify(emailSender, only()).sendMessage(anyString(), anyString(), anyCollection());
        assertEquals(1, notificationsDao.getActualNotifications().size());

        final DiListResponse<DiEntity> entities = dispenser().getEntities().inService(NIRVANA).perform();
        dispenser().quotas().changeInService(NIRVANA).releaseEntities(entities).perform();
        sendNotifications();

        // no new notifications sent, no actual notifications
        verify(emailSender, only()).sendMessage(anyString(), anyString(), anyCollection());
        assertEquals(0, notificationsDao.getActualNotifications().size());

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));
        sendNotifications();

        // 2 notifications sent in total, 1 actual notification
        verify(emailSender, times(2)).sendMessage(anyString(), anyString(), anyCollection());
        verifyNoMoreInteractions(emailSender);
        assertEquals(1, notificationsDao.getActualNotifications().size());
    }

    @Test
    public void notificationsShouldBeSentOnlyForConfiguredQuotas() {
        skipIfNeeded();

        notificationManager.setNotificationFilteringPredicate(parseNotificationFilteringPredicate(
                NIRVANA + ", " + SCRAPER + "/" + DOWNLOADS + "/" + DOWNLOADS_PER_DAY));

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));
        // causes overquoting of both "downloads_per_day" and "downloads_per_hour" quotas for "verticali" project
        dispenser().quotas()
                .changeInService(SCRAPER)
                .acquireResource(DiResourceAmount.ofResource(DOWNLOADS).withAmount(50, DiUnit.COUNT).build(), LYADZHIN.chooses(VERTICALI))
                .perform();

        sendNotifications();

        verify(emailSender, times(2)).sendMessage(anyString(), anyString(), anyCollection());
        verifyNoMoreInteractions(emailSender);

        final Set<NotificationEntry> actualNotifications = notificationsDao.getActualNotifications();
        assertEquals(2, actualNotifications.size());
        assertTrue(actualNotifications.stream().anyMatch(n -> INFRA.equals(n.getProject().getPublicKey())
                && NIRVANA.equals(n.getSpec().getResource().getService().getKey())
                && STORAGE.equals(n.getSpec().getResource().getKey().getPublicKey())));
        assertTrue(actualNotifications.stream().anyMatch(n -> VERTICALI.equals(n.getProject().getPublicKey())
                && SCRAPER.equals(n.getSpec().getResource().getService().getKey())
                && DOWNLOADS.equals(n.getSpec().getResource().getKey().getPublicKey())
                && DOWNLOADS_PER_DAY.equals(n.getSpec().getKey().getPublicKey())));
        assertTrue(actualNotifications.stream().noneMatch(n -> VERTICALI.equals(n.getProject().getPublicKey())
                && SCRAPER.equals(n.getSpec().getResource().getService().getKey())
                && DOWNLOADS.equals(n.getSpec().getResource().getKey().getPublicKey())
                && DOWNLOADS_PER_HOUR.equals(n.getSpec().getKey().getPublicKey())));
    }

    @Test
    public void notificationsShouldNotBeSentForQuotasWithZeroActualAndMaxValues() {
        skipIfNeeded();

        final DiQuotaGetResponse quotas = dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(0, DiUnit.BYTE)).build())
                .performBy(WHISTLER);
        assertEquals(quotas.getFirst().getMax(), DiAmount.of(0, DiUnit.BYTE));

        sendNotifications();

        verify(emailSender, never()).sendMessage(anyString(), anyString(), anyCollection());
        assertThat(notificationsDao.getActualNotifications(), is(empty()));
    }

    @Test
    public void noNotificationsShouldBeSentIfNotificationFilteringPredicateIsEmpty() {
        skipIfNeeded();

        notificationManager.setNotificationFilteringPredicate(parseNotificationFilteringPredicate(""));

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));
        dispenser().quotas()
                .changeInService(SCRAPER)
                .acquireResource(DiResourceAmount.ofResource(DOWNLOADS).withAmount(50, DiUnit.COUNT).build(), LYADZHIN.chooses(VERTICALI))
                .perform();

        sendNotifications();

        verify(emailSender, never()).sendMessage(anyString(), anyString(), anyCollection());
        assertThat(notificationsDao.getActualNotifications(), is(empty()));
    }

    @Test
    public void notificationShouldHaveProperTextAndRecipientsIfSentToProjectResponsibles() {
        skipIfNeeded();

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));
        sendNotifications();

        final String text = "Вы получили это письмо, так как являетесь ответственным за проект"
                + " \"Infrastruktura\" в Dispenser (https://dispenser-dev.yandex-team.ru/projects/infra).\n"
                + "Потребление по квоте \"Nirvana storage quota.\" в сервисе \"Nirvana\" для проекта \"Infrastruktura\" превысило лимит в 100 %";
        verify(emailSender, only()).sendMessage(any(), contains(text), eq(Collections.singleton(WHISTLER.getLogin() + "@yandex-team.ru")));
    }

    @Test
    public void notificationShouldHaveProperTextAndRecipientsIfSentToServiceAdmins() {
        skipIfNeeded();

        projectDao.detach(personDao.read(WHISTLER.getLogin()), projectDao.read(YANDEX), Role.RESPONSIBLE);

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));
        sendNotifications();

        final String text = "Вы получили это письмо, так как являетесь администратором сервиса \"Nirvana\" в Dispenser.\n"
                + "Потребление по квоте \"Nirvana storage quota.\" в сервисе \"Nirvana\" для проекта \"Infrastruktura\""
                + " (https://dispenser-dev.yandex-team.ru/projects/infra) превысило лимит в 100 %";
        verify(emailSender, only()).sendMessage(any(), contains(text), eq(Collections.singleton(SANCHO.getLogin() + "@yandex-team.ru")));
    }

    @Test
    public void notificationShouldHaveProperSubject() {
        skipIfNeeded();

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));
        sendNotifications();

        verify(emailSender, only()).sendMessage(eq("[Nirvana] Infrastruktura: исчерпание квоты"), any(), any());
    }

    @Test
    public void notificationTextShouldContainOverquotingDateTime() {
        skipIfNeeded();

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));

        HierarchyRequiredAspect.runWithDisabledCheck(() -> lotsManager.update());
        sendNotifications();

        updateHierarchy();
        final DiQuota quota = dispenser().quotas().get().inService(NIRVANA).forResource(STORAGE).ofProject(INFRA).perform().getFirst();
        final long overquotingTs = Objects.requireNonNull(quota.getLastOverquotingTs());

        final String substring = DateTimeUtils.formatToMskDateTime(overquotingTs);
        verify(emailSender, only()).sendMessage(any(), contains(substring), any());
    }


    @Test
    public void quotaNotificationMustBeSendedToMlIfSpecified() {
        skipIfNeeded();

        projectDao.update(Project.copyOf(projectDao.read(INFRA))
                .mailList("to-infra@yandex-team.ru")
                .build());
        updateHierarchy();

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));

        HierarchyRequiredAspect.runWithDisabledCheck(() -> lotsManager.update());
        sendNotifications();

        updateHierarchy();
        verify(emailSender, only()).sendMessage(anyString(), anyString(), eq(ImmutableSet.of("to-infra@yandex-team.ru")));
    }

    @Test
    public void quotaNotificationMustBeSendedToMlAndResponsiblesIfSpecified() {
        skipIfNeeded();

        projectDao.update(Project.copyOf(projectDao.read(YANDEX))
                .mailList("to-yandex@yandex-team.ru")
                .build());
        updateHierarchy();

        createNirvanaYtFileOfSize(DiAmount.of(100, DiUnit.BYTE));

        HierarchyRequiredAspect.runWithDisabledCheck(() -> lotsManager.update());
        sendNotifications();

        updateHierarchy();
        verify(emailSender, only()).sendMessage(anyString(), anyString(), eq(ImmutableSet.of("whistler@yandex-team.ru", "to-yandex@yandex-team.ru")));
    }

}
