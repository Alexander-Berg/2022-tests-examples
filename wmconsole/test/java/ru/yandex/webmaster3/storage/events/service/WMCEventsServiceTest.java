package ru.yandex.webmaster3.storage.events.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.events.data.WMCEvent;
import ru.yandex.webmaster3.storage.events.data.WMCEventContent;
import ru.yandex.webmaster3.storage.events.data.events.TurboSettingsChangeEvent;
import ru.yandex.webmaster3.storage.events.data.events.UserHostMessageEvent;
import ru.yandex.webmaster3.storage.user.message.content.MessageContent;
import ru.yandex.webmaster3.storage.user.message.iks.IksMessageContent;
import ru.yandex.webmaster3.storage.user.message.iks.IksMessageType;
import ru.yandex.webmaster3.storage.user.notification.NotificationType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * ishalaru
 * 18.12.2020
 **/
public class WMCEventsServiceTest {

    @Test
    public void test() {
        LbWMCEventService lbWmcEventService = mock(LbWMCEventService.class);
        WMCEventsService wmcEventsService = new WMCEventsService(lbWmcEventService);
        final UserHostMessageEvent<MessageContent.IksNewInfo> event1 = new UserHostMessageEvent<>(
                IdUtils.stringToHostId("http:lenta.ru:80"),
                Long.valueOf(1l),
                new MessageContent.IksNewInfo(List.of(), List.of(), IdUtils.stringToHostId("http:lenta.ru:80"),
                        new IksMessageContent.IksRival(1L, List.of()),
                        IksMessageType.RIVAL_UPDATE,
                        "test"),
                NotificationType.IKS_UPDATE,
                false);
        final UserHostMessageEvent<MessageContent.NewReviewAvailable> event2 = new UserHostMessageEvent<>(
                IdUtils.stringToHostId("http:lenta.ru:80"),
                Long.valueOf(1l),
                new MessageContent.NewReviewAvailable(IdUtils.stringToHostId("http:lenta.ru:80"), List.of(), 0, 0, "st"),
                NotificationType.NEW_REVIEW_AVAILABLE,
                false);

        final TurboSettingsChangeEvent event3 = new TurboSettingsChangeEvent("list.ru", null, null, null, null, null, null);
        wmcEventsService.addEvents(List.of(WMCEvent.create(event1), WMCEvent.create(event2), WMCEvent.create(event3)));
        ArgumentCaptor<List<WMCEvent>> listArgumentCaptor2 = ArgumentCaptor.forClass(List.class);
        verify(lbWmcEventService).sendWmcEvent(listArgumentCaptor2.capture());
        Assert.assertEquals(3, listArgumentCaptor2.getAllValues().get(0).size());
    }

    @Test
    public void testIndividualOne() {
        LbWMCEventService lbWmcEventService = mock(LbWMCEventService.class);
        WMCEventsService wmcEventsService = new WMCEventsService(lbWmcEventService);
        final UserHostMessageEvent<MessageContent.IksNewInfo> event1 = new UserHostMessageEvent<>(
                IdUtils.stringToHostId("http:lenta.ru:80"),
                Long.valueOf(1l),
                new MessageContent.IksNewInfo(List.of(), List.of(), IdUtils.stringToHostId("http:lenta.ru:80"),
                        new IksMessageContent.IksRival(1L, List.of()),
                        IksMessageType.RIVAL_UPDATE,
                        "test"),
                NotificationType.IKS_UPDATE,
                false);
        wmcEventsService.addEvent(event1);
        ArgumentCaptor<WMCEvent> listArgumentCaptor = ArgumentCaptor.forClass(WMCEvent.class);
        Assert.assertEquals(0, listArgumentCaptor.getAllValues().size());
        ArgumentCaptor<List<WMCEventContent>> listArgumentCaptor2 = ArgumentCaptor.forClass(List.class);
        verify(lbWmcEventService).send(listArgumentCaptor2.capture());
        Assert.assertEquals(1, listArgumentCaptor2.getAllValues().get(0).size());


    }

    @Test
    public void testIndividualTwo() {
        LbWMCEventService lbWmcEventService = mock(LbWMCEventService.class);
        WMCEventsService wmcEventsService = new WMCEventsService(lbWmcEventService);
        final UserHostMessageEvent<MessageContent.NewReviewAvailable> event2 = new UserHostMessageEvent<>(
                IdUtils.stringToHostId("http:lenta.ru:80"),
                Long.valueOf(1l),
                new MessageContent.NewReviewAvailable(IdUtils.stringToHostId("http:lenta.ru:80"), List.of(), 0, 0, "st"),
                NotificationType.NEW_REVIEW_AVAILABLE,
                false);
        wmcEventsService.addEvent(event2);
        ArgumentCaptor<WMCEvent> listArgumentCaptor = ArgumentCaptor.forClass(WMCEvent.class);
        ArgumentCaptor<List<WMCEventContent>> listArgumentCaptor2 = ArgumentCaptor.forClass(List.class);
        verify(lbWmcEventService, times(1)).send(listArgumentCaptor2.capture());
        Assert.assertEquals(1, listArgumentCaptor2.getAllValues().size());

    }

    @Test
    public void testIndividualTree() {
        LbWMCEventService lbWmcEventService = mock(LbWMCEventService.class);
        WMCEventsService wmcEventsService = new WMCEventsService(lbWmcEventService);
        final TurboSettingsChangeEvent event3 = new TurboSettingsChangeEvent("list.ru", null, null, null, null, null, null);
        wmcEventsService.addEvent(event3);
        ArgumentCaptor<List<WMCEventContent>> listArgumentCaptor2 = ArgumentCaptor.forClass(List.class);
        verify(lbWmcEventService).send(listArgumentCaptor2.capture());
        Assert.assertEquals(1, listArgumentCaptor2.getAllValues().get(0).size());

    }

}
