package ru.yandex.webmaster3.worker.notifications.auto;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.notification.LanguageEnum;
import ru.yandex.webmaster3.storage.events.data.WMCEvent;
import ru.yandex.webmaster3.storage.events.data.WMCEventType;
import ru.yandex.webmaster3.storage.events.data.events.UserHostMessageEvent;
import ru.yandex.webmaster3.storage.notifications.NotificationChannel;
import ru.yandex.webmaster3.storage.notifications.UserNotificationConfiguration;
import ru.yandex.webmaster3.storage.notifications.service.UserNotificationSettingsService;
import ru.yandex.webmaster3.storage.spam.ISpamHostFilter;
import ru.yandex.webmaster3.storage.user.UserPersonalInfo;
import ru.yandex.webmaster3.storage.user.message.content.MessageContent;
import ru.yandex.webmaster3.storage.user.notification.NotificationType;
import ru.yandex.webmaster3.storage.user.service.UserPersonalInfoService;
import ru.yandex.webmaster3.worker.notifications.EuEmailService;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author: ishalaru
 * DATE: 08.07.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class AutoNotificationsObserverTest {

    private AutoNotificationsObserver autoNotificationsObserver;
    @Mock
    private UserNotificationSettingsService userNotificationSettingsService;
    @Mock
    private AutoNotificationsSenderService autoNotificationsSenderService;
    @Mock
    private UserPersonalInfoService userPersonalInfoService;
    @Mock
    private ISpamHostFilter spamHostFilter;
    @Mock
    private EuEmailService euEmailService;
    @Mock
    private ExtendedInfoService extendedInfoService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        autoNotificationsObserver = new AutoNotificationsObserver(
                autoNotificationsSenderService,
                euEmailService,
                spamHostFilter,
                userNotificationSettingsService,
                userPersonalInfoService,
                null,
                extendedInfoService
        );
        autoNotificationsObserver.setSendByChannel(NotificationChannel.EMAIL);
    }

    @Test
    public void messageIsValidAndShouldBeSent() {
        String host = "test.ru";
        WebmasterHostId webmasterHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, host, 80);
        var content = new MessageContent.TurboAutoparsedPagesAppeared(webmasterHostId);
        var userMessageEvent = new UserHostMessageEvent<>(
                webmasterHostId,
                1L,
                content,
                NotificationType.TURBO_NEW,
                false);
        UUID uuid = UUID.randomUUID();
        //WebmasterCommonProto.UUID.getDefaultInstance();
        WMCEvent wmcEvent = new WMCEvent(uuid, WMCEventType.RETRANSLATE_TO_USERS, userMessageEvent, 1L);
        UserNotificationConfiguration userNotificationConfiguration = new UserNotificationConfiguration(Set.of(NotificationChannel.SERVICE, NotificationChannel.EMAIL), "test@yandex.ru");
        when(userNotificationSettingsService.getUserNotificationsSettings(1L, NotificationType.TURBO_NEW))
                .thenReturn(userNotificationConfiguration);
        //when(euEmailService.isAddressEuropean("test@yandex.ru")).thenReturn(false);
        UserPersonalInfo userPersonalInfo = new UserPersonalInfo(1L, "test", "test tt", LanguageEnum.RU);
        when(userPersonalInfoService.getUsersPersonalInfos(Collections.singleton(1L))).thenReturn(Map.of(1L, userPersonalInfo));
        autoNotificationsObserver.observe(wmcEvent);
        NotificationInfo notificationInfo = NotificationInfo.builder()
                .id(uuid)
                .email("test@yandex.ru")
                .hostId(webmasterHostId)
                .userId(1L)
                .personalInfo(userPersonalInfo)
                .messageContent(content)
                .channel(NotificationChannel.EMAIL)
                .critical(false)
                .build();
        verify(autoNotificationsSenderService, times(1)).
                sendMessage(notificationInfo);
    }

    @Test
    public void emailIsInEuropeanList() {
        String host = "test.ru";
        WebmasterHostId webmasterHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, host, 80);
        var content = new MessageContent.TurboAutoparsedPagesAppeared(webmasterHostId);
        var userMessageEvent = new UserHostMessageEvent<>(
                webmasterHostId,
                1L,
                content,
                NotificationType.TURBO_LISTINGS_NEW,
                false);
        UUID uuid = UUID.randomUUID();
        //WebmasterCommonProto.UUID.getDefaultInstance();
        WMCEvent wmcEvent = new WMCEvent(uuid, WMCEventType.RETRANSLATE_TO_USERS, userMessageEvent, 1L);
        UserNotificationConfiguration userNotificationConfiguration = new UserNotificationConfiguration(Set.of(NotificationChannel.SERVICE), "test@yandex.ru");
        when(userNotificationSettingsService.getUserNotificationsSettings(1L, NotificationType.TURBO_LISTINGS_NEW))
                .thenReturn(userNotificationConfiguration);
        when(euEmailService.isAddressEuropean("test@yandex.ru")).thenReturn(true);
        UserPersonalInfo userPersonalInfo = new UserPersonalInfo(1L, "test", "test tt", LanguageEnum.RU);
        //when(userPersonalInfoService.getUsersPersonalInfos(Collections.singleton(1L))).thenReturn(Map.of(1L,userPersonalInfo));
        autoNotificationsObserver.observe(wmcEvent);
        NotificationInfo notificationInfo = NotificationInfo.builder()
                .id(uuid)
                .email("test@yandex.ru")
                .hostId(webmasterHostId)
                .userId(1L)
                .personalInfo(userPersonalInfo)
                .messageContent(content)
                .channel(NotificationChannel.EMAIL)
                .critical(false)
                .build();
        verify(autoNotificationsSenderService, never()).
                sendMessage(notificationInfo);
    }

    @Test
    public void notificationNotFromServiceList() {
        String host = "test.ru";
        WebmasterHostId webmasterHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, host, 80);
        var content = new MessageContent.TurboAutoparsedPagesAppeared(webmasterHostId);
        var userMessageEvent = new UserHostMessageEvent<>(
                webmasterHostId,
                1L,
                content,
                NotificationType.GLOBAL_NOTIFICATION,
                false);
        UUID uuid = UUID.randomUUID();
        //WebmasterCommonProto.UUID.getDefaultInstance();
        WMCEvent wmcEvent = new WMCEvent(uuid, WMCEventType.RETRANSLATE_TO_USERS, userMessageEvent, 1L);
        UserNotificationConfiguration userNotificationConfiguration = new UserNotificationConfiguration(Set.of(NotificationChannel.SERVICE), "test@yandex.ru");
        when(userNotificationSettingsService.getUserNotificationsSettings(1L, NotificationType.GLOBAL_NOTIFICATION))
                .thenReturn(userNotificationConfiguration);
        UserPersonalInfo userPersonalInfo = new UserPersonalInfo(1L, "test", "test tt", LanguageEnum.RU);
        //when(userPersonalInfoService.getUsersPersonalInfos(Collections.singleton(1L))).thenReturn(Map.of(1L,userPersonalInfo));
        autoNotificationsObserver.observe(wmcEvent);
        NotificationInfo notificationInfo = NotificationInfo.builder()
                .id(uuid)
                .email("test@yandex.ru")
                .hostId(webmasterHostId)
                .userId(1L)
                .personalInfo(userPersonalInfo)
                .messageContent(content)
                .channel(NotificationChannel.EMAIL)
                .critical(false)
                .build();
        verify(autoNotificationsSenderService, never()).
                sendMessage(notificationInfo);
    }
}
