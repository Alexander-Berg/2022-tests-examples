package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.error.EntityNotFoundException;
import ru.yandex.vertis.releaseissuenotifier.bean.PushBean;
import ru.yandex.vertis.releaseissuenotifier.bean.StarTrackTag;
import ru.yandex.vertis.releaseissuenotifier.startrack.CloseTicketsListAction;
import ru.yandex.vertis.releaseissuenotifier.startrack.FilterBuilder;
import ru.yandex.vertis.releaseissuenotifier.startrack.Ticket;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class CloseTicketsListActionTest {

    @Test
    public void shouldReturnCorrectInstanceOfCloseTicketsListAction() {
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        assertThat("result object is not CloseTicketsListAction", closeTicketsListAction, isA(CloseTicketsListAction.class));
    }

    @Test
    public void shouldReturnCloseTicketsListActionInstanceWithEmptyTicketsList() {
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        assertThat("tickets list is not empty", closeTicketsListAction.getTickets(), is(emptyCollectionOf(Ticket.class)));
    }

    @Test
    public void shouldReturnCloseTicketsListActionInstanceWithEmptySession() {
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        assertThat("session is not null", closeTicketsListAction.getSession(), is(nullValue()));
    }

    @Test
    public void shouldReturnCloseTicketsListActionInstanceWithEmptyReleaseTicket() {
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        assertThat("releaseTicketKey is not null", closeTicketsListAction.getReleaseTicketKey(), is(nullValue()));
    }

    @Test
    public void shouldReturnCloseTicketsListActionInstanceWithEmptyFilter() {
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        String filter = getPrivateField("filter", closeTicketsListAction);
        assertThat("filter is not null", filter, is(nullValue()));
    }

    @Test
    public void shouldReturnCloseTicketsListActionInstanceWithEmptyFilterBuilder() {
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        FilterBuilder filterBuilder = getPrivateField("filterBuilder", closeTicketsListAction);
        assertThat("filter is not null", filterBuilder.getFilter(), isEmptyOrNullString());
    }

    @Test
    public void shouldSetSession() {
        Session mockSession = Mockito.mock(Session.class);
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        closeTicketsListAction.setSession(mockSession);

        assertThat("Session is not set correctly", closeTicketsListAction.getSession(), equalTo(mockSession));
    }

    @Test
    public void shouldSetTag() {
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        closeTicketsListAction.setTag(StarTrackTag.RELEASE_FORMING);

        assertThat("Tag is not set correctly", closeTicketsListAction.getTag().getTag(), equalTo("release_forming"));
    }


    @Test
    public void shouldSetReleaseTicketKey() {
        String releaseTicketKey = "AUTORUAPPS-0000";
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        closeTicketsListAction.setReleaseTicketKey(releaseTicketKey);

        assertThat("ReleaseTicketKey is not set correctly", closeTicketsListAction.getReleaseTicketKey(), equalTo(releaseTicketKey));
    }

    @Test
    public void shouldSetTicketsList() {
        List<Ticket> ticketsList = Arrays.asList(Ticket.ticket(), Ticket.ticket());
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();

        closeTicketsListAction.setTickets(ticketsList);

        assertThat("Tickets list is not set correctly", closeTicketsListAction.getTickets(), equalTo(ticketsList));
    }

    @Test
    public void shouldSetupReleaseTicketWithSession() {
        Session mockSession = Mockito.mock(Session.class);
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();
        closeTicketsListAction.setSession(mockSession);

        closeTicketsListAction.setupReleaseTickets();

        assertThat("Release ticket setup not correctly", closeTicketsListAction.getReleaseTicket().getSession(), equalTo(mockSession));
        assertThat("Release ticket is instance of a Ticket class", closeTicketsListAction.getReleaseTicket(), isA(Ticket.class));
    }

    @Test
    public void shouldSetupFixVersion() {
        String mockVersionID = "123456";
        String mockReleaseTicketKey = "AUTORUAPPS-0000";
        Session mockSession = Mockito.mock(Session.class);
        Ticket mockTicket = Mockito.mock(Ticket.class);
        when(mockTicket.getIssueByNumber(anyString())).thenReturn(mockTicket);
        when(mockTicket.getExactlyOneFixVersionId()).thenReturn(mockVersionID);
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();
        closeTicketsListAction.setSession(mockSession);
        closeTicketsListAction.setReleaseTicketKey(mockReleaseTicketKey);
        closeTicketsListAction.setReleaseTicket(mockTicket);

        closeTicketsListAction.setupFixVersion();

        assertThat("Fix version setup no correct", closeTicketsListAction.getFixVersion(), equalTo(mockVersionID));
    }

    @Test
    public void shouldNotSetupFixVersionWithoutErrorThrowsAndLoggingIfTicketNotFound() {
        String mockReleaseTicketKey = "AUTORUAPPS-0000";
        Session mockSession = Mockito.mock(Session.class);
        Ticket mockTicket = Mockito.mock(Ticket.class);
        Logger mockLogger = Mockito.mock(Logger.class);
        EntityNotFoundException mockNotFoundError = Mockito.mock(EntityNotFoundException.class);
        when(mockNotFoundError.toString()).thenReturn("mocked error message");
        when(mockTicket.getIssueByNumber(anyString())).thenThrow(mockNotFoundError);
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();
        closeTicketsListAction.setSession(mockSession);
        closeTicketsListAction.setReleaseTicketKey(mockReleaseTicketKey);
        closeTicketsListAction.setReleaseTicket(mockTicket);
        setPrivateField("log", closeTicketsListAction, mockLogger);

        closeTicketsListAction.setupFixVersion();

        assertThat("Fix version setup not correct", closeTicketsListAction.getFixVersion(), is(nullValue()));
        verify(mockLogger, times(1)).error(anyString());
    }

    @Test
    public void shouldBuildFilterWithTagWhenTagIsNotNull() {
        FilterBuilder mockFilterBuilder = Mockito.mock(FilterBuilder.class);
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();
        closeTicketsListAction.setTag(StarTrackTag.RELEASE_FORMING);
        setPrivateField("filterBuilder", closeTicketsListAction, mockFilterBuilder);

        closeTicketsListAction.setupFilter();

        verify(mockFilterBuilder, times(1)).addTag(StarTrackTag.RELEASE_FORMING);
        verify(mockFilterBuilder, never()).addFixVersions(anyString());
        verify(mockFilterBuilder, never()).addQueue(anyString());
    }

    @Test
    public void shouldBuildFilterWithFixVersionWhenFixVersionIsNotNull() {
        String mockVersionID = "123456";
        FilterBuilder mockFilterBuilder = Mockito.mock(FilterBuilder.class);
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();
        closeTicketsListAction.setFixVersion(mockVersionID);
        setPrivateField("filterBuilder", closeTicketsListAction, mockFilterBuilder);

        closeTicketsListAction.setupFilter();

        verify(mockFilterBuilder, times(1)).addFixVersions(mockVersionID);
        verify(mockFilterBuilder, never()).addTag(any());
        verify(mockFilterBuilder, never()).addQueue(anyString());
    }

    @Test
    public void shouldBuildFilterWithQueueWhenReleaseTicketIsNotNull() {
        String mockReleaseQueue = "MOCK_QUEUE";
        Ticket mockTicket = Mockito.mock(Ticket.class);
        when(mockTicket.getQueue()).thenReturn(mockReleaseQueue);
        FilterBuilder mockFilterBuilder = Mockito.mock(FilterBuilder.class);
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();
        closeTicketsListAction.setReleaseTicket(mockTicket);
        setPrivateField("filterBuilder", closeTicketsListAction, mockFilterBuilder);

        closeTicketsListAction.setupFilter();

        verify(mockFilterBuilder, times(1)).addQueue(mockReleaseQueue);
        verify(mockFilterBuilder, never()).addFixVersions(anyString());
        verify(mockFilterBuilder, never()).addTag(any());
    }

    @Test
    public void shouldCloseAllCurrentTickets() {
        Ticket mockTicket1 = Mockito.mock(Ticket.class);
        Ticket mockTicket2 = Mockito.mock(Ticket.class);
        Logger mockLogger = Mockito.mock(Logger.class);
        List<Ticket> ticketsList = Arrays.asList(mockTicket1, mockTicket2);
        CloseTicketsListAction closeTicketsListAction = CloseTicketsListAction.closeTicketsListAction();
        closeTicketsListAction.setTickets(ticketsList);
        setPrivateField("log", closeTicketsListAction, mockLogger);

        closeTicketsListAction.closeAllTickets();

        ticketsList.forEach(ticket -> verify(ticket, times(1)).close());
    }

    @Test
    public void shouldSetReleaseFormingTagAndSetupTicketsWhenReleaseTicketInReadyStatus() {
        PushBean testPushBean = new PushBean().setStatus("Ready for Release");
        CloseTicketsListAction closeTicketsListAction = Mockito.spy(CloseTicketsListAction.class);
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setReleaseTicketKey(anyString());
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setupReleaseTickets();
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setupFixVersion();
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setupFilter();
        doReturn(closeTicketsListAction).when(closeTicketsListAction).prepareTicketsByFilter();
        doNothing().when(closeTicketsListAction).closeAllTickets();
        closeTicketsListAction.setPush(testPushBean);

        closeTicketsListAction.closeTickets();

        assertThat("release forming tag is not set", closeTicketsListAction.getTag(), is(StarTrackTag.RELEASE_FORMING));
        verify(closeTicketsListAction, times(1)).setReleaseTicketKey(any());
        verify(closeTicketsListAction, times(1)).setupReleaseTickets();
        verify(closeTicketsListAction, times(1)).setupFixVersion();
        verify(closeTicketsListAction, times(1)).setupFilter();
        verify(closeTicketsListAction, times(1)).prepareTicketsByFilter();
        verify(closeTicketsListAction, times(1)).closeAllTickets();
    }

    @Test
    public void shouldNotSetReleaseFormingTagAndSetupTicketsWhenReleaseTicketInReadyStatus() {
        PushBean testPushBean = new PushBean().setStatus("Закрыт");
        CloseTicketsListAction closeTicketsListAction = Mockito.spy(CloseTicketsListAction.class);
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setReleaseTicketKey(anyString());
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setupReleaseTickets();
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setupFixVersion();
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setupFilter();
        doReturn(closeTicketsListAction).when(closeTicketsListAction).prepareTicketsByFilter();
        doNothing().when(closeTicketsListAction).closeAllTickets();
        closeTicketsListAction.setPush(testPushBean);

        closeTicketsListAction.closeTickets();

        assertThat("release forming tag is set", closeTicketsListAction.getTag(), is(nullValue()));
        verify(closeTicketsListAction, times(1)).setReleaseTicketKey(any());
        verify(closeTicketsListAction, times(1)).setupReleaseTickets();
        verify(closeTicketsListAction, times(1)).setupFixVersion();
        verify(closeTicketsListAction, times(1)).setupFilter();
        verify(closeTicketsListAction, times(1)).prepareTicketsByFilter();
        verify(closeTicketsListAction, times(1)).closeAllTickets();
    }

    @Test
    public void shouldNotSetupAndCloseTicketsWhenStatusNotReadyOrClose() {
        PushBean testPushBean = new PushBean().setStatus("Smoke");
        CloseTicketsListAction closeTicketsListAction = Mockito.spy(CloseTicketsListAction.class);
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setReleaseTicketKey(anyString());
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setupReleaseTickets();
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setupFixVersion();
        doReturn(closeTicketsListAction).when(closeTicketsListAction).setupFilter();
        doReturn(closeTicketsListAction).when(closeTicketsListAction).prepareTicketsByFilter();
        doNothing().when(closeTicketsListAction).closeAllTickets();
        closeTicketsListAction.setPush(testPushBean);

        closeTicketsListAction.closeTickets();

        assertThat("release forming tag is set", closeTicketsListAction.getTag(), is(nullValue()));
        verify(closeTicketsListAction, times(0)).setReleaseTicketKey(any());
        verify(closeTicketsListAction, times(0)).setupReleaseTickets();
        verify(closeTicketsListAction, times(0)).setupFixVersion();
        verify(closeTicketsListAction, times(0)).setupFilter();
        verify(closeTicketsListAction, times(0)).prepareTicketsByFilter();
        verify(closeTicketsListAction, times(0)).closeAllTickets();
    }

    private <T> void setPrivateField(String filedName, CloseTicketsListAction closeTicketsListActionInstance, T settingValue) {
        try {
            Field field = closeTicketsListActionInstance.getClass().getDeclaredField(filedName);
            field.setAccessible(true);
            field.set(closeTicketsListActionInstance, settingValue);

        } catch (Exception e) {
            Assert.fail(String.format("Cannot set value to filed with name %s: ", filedName) + e);
        }
    }

    private <T> T getPrivateField(String filedName, CloseTicketsListAction closeTicketsListActionInstance) {
        try {
            Field field = closeTicketsListActionInstance.getClass().getDeclaredField(filedName);
            field.setAccessible(true);
            return (T) field.get(closeTicketsListActionInstance);

        } catch (Exception e) {
            Assert.fail(String.format("Cannot get filed with name %s: ", filedName) + e.getMessage());
        }
        return null;
    }
}
