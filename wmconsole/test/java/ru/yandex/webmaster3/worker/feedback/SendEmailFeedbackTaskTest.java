package ru.yandex.webmaster3.worker.feedback;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.feedback.FeedbackType;
import ru.yandex.webmaster3.core.feedback.SendFeedbackEMailTaskData;
import ru.yandex.webmaster3.core.worker.task.TaskResult;
import ru.yandex.webmaster3.storage.notifications.service.EmailSenderService;
import ru.yandex.webmaster3.storage.notifications.service.UserNotificationSettingsService;
import ru.yandex.webmaster3.worker.Task;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author: ishalaru
 * DATE: 02.07.2019
 */
public class SendEmailFeedbackTaskTest {

    @Test
    public void oneEmailSendSuccess() throws Exception {
        EmailSenderService emailSenderService = mock(EmailSenderService.class);
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        UserNotificationSettingsService userNotificationSettingsService = mock(UserNotificationSettingsService.class);
        SendEmailFeedbackTask sendEmailFeedbackTask = new SendEmailFeedbackTask(Map.of("test@yandex.ru", List.of(), "test@yandex-team.ru", List.of()), Map.of(),
                emailSenderService,
                userNotificationSettingsService);

        WebmasterHostId hostId = WebmasterHostId.createNoLowerCase(WebmasterHostId.Schema.HTTP, "test", 80);
        SendFeedbackEMailTaskData sendFeedbackEMailTaskData = new SendFeedbackEMailTaskData(hostId, 1232L, "test", null, FeedbackType.AUTOMORDA_OFF);
        Task.Result run = sendEmailFeedbackTask.run(sendFeedbackEMailTaskData);
        Assert.assertEquals("Task result", run.getTaskResult(), TaskResult.SUCCESS);
        verify(emailSenderService, times(2)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void emailServerProblem() throws Exception {
        EmailSenderService emailSenderService = mock(EmailSenderService.class);
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(false);
        UserNotificationSettingsService userNotificationSettingsService = mock(UserNotificationSettingsService.class);
        SendEmailFeedbackTask sendEmailFeedbackTask = new SendEmailFeedbackTask(Map.of("test@yandex.ru", List.of(), "test@yandex-team.ru", List.of()), Map.of(),
                emailSenderService,
                userNotificationSettingsService);

        WebmasterHostId hostId = WebmasterHostId.createNoLowerCase(WebmasterHostId.Schema.HTTP, "test", 80);
        SendFeedbackEMailTaskData sendFeedbackEMailTaskData = new SendFeedbackEMailTaskData(hostId, 1232L, "test", null, FeedbackType.AUTOMORDA_OFF);
        Task.Result run = sendEmailFeedbackTask.run(sendFeedbackEMailTaskData);
        Assert.assertEquals("Task result", run.getTaskResult(), TaskResult.UNKNOWN);
        verify(emailSenderService, times(2 * SendEmailFeedbackTask.MAX_RETRY_ATTEMPT)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void emailSendOnlySecondTimes() throws Exception {
        EmailSenderService emailSenderService = mock(EmailSenderService.class);
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(false).thenReturn(false).thenReturn(true).thenReturn(false).thenReturn(true);
        UserNotificationSettingsService userNotificationSettingsService = mock(UserNotificationSettingsService.class);
        SendEmailFeedbackTask sendEmailFeedbackTask = new SendEmailFeedbackTask(Map.of("test@yandex.ru", List.of(), "test@yandex-team.ru", List.of()), Map.of(),
                emailSenderService,
                userNotificationSettingsService);

        WebmasterHostId hostId = WebmasterHostId.createNoLowerCase(WebmasterHostId.Schema.HTTP, "test", 80);
        SendFeedbackEMailTaskData sendFeedbackEMailTaskData = new SendFeedbackEMailTaskData(hostId, 1232L, "test", null, FeedbackType.AUTOMORDA_OFF);
        Task.Result run = sendEmailFeedbackTask.run(sendFeedbackEMailTaskData);
        Assert.assertEquals("Task result", run.getTaskResult(), TaskResult.SUCCESS);
        verify(emailSenderService, times(5)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void emailSendOnlyOneMessage() throws Exception {
        EmailSenderService emailSenderService = mock(EmailSenderService.class);
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true).thenReturn(false);
        UserNotificationSettingsService userNotificationSettingsService = mock(UserNotificationSettingsService.class);
        SendEmailFeedbackTask sendEmailFeedbackTask = new SendEmailFeedbackTask(Map.of("test@yandex.ru", List.of(), "test@yandex-team.ru", List.of()), Map.of(),
                emailSenderService,
                userNotificationSettingsService);


        WebmasterHostId hostId = WebmasterHostId.createNoLowerCase(WebmasterHostId.Schema.HTTP, "test", 80);
        SendFeedbackEMailTaskData sendFeedbackEMailTaskData = new SendFeedbackEMailTaskData(hostId, 1232L, "test", null, FeedbackType.AUTOMORDA_OFF);
        Task.Result run = sendEmailFeedbackTask.run(sendFeedbackEMailTaskData);
        Assert.assertEquals("Task result", run.getTaskResult(), TaskResult.UNKNOWN);
        verify(emailSenderService, times(1 + SendEmailFeedbackTask.MAX_RETRY_ATTEMPT)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void filterEmailByFeedbackType() throws Exception {
        EmailSenderService emailSenderService = mock(EmailSenderService.class);
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        UserNotificationSettingsService userNotificationSettingsService = mock(UserNotificationSettingsService.class);
        SendEmailFeedbackTask sendEmailFeedbackTask = new SendEmailFeedbackTask(Map.of("test@yandex.ru", List.of("TURBO_SUPPORT_QUERY"), "test@yandex-team.ru", List.of()), Map.of(),
                emailSenderService,
                userNotificationSettingsService);

        WebmasterHostId hostId = WebmasterHostId.createNoLowerCase(WebmasterHostId.Schema.HTTP, "test", 80);
        SendFeedbackEMailTaskData sendFeedbackEMailTaskData = new SendFeedbackEMailTaskData(hostId, 1232L, "test", null, FeedbackType.AUTOMORDA_OFF);
        Task.Result run = sendEmailFeedbackTask.run(sendFeedbackEMailTaskData);
        Assert.assertEquals("Task result", run.getTaskResult(), TaskResult.SUCCESS);
        verify(emailSenderService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void sendEmailByFeedbackType() throws Exception {
        EmailSenderService emailSenderService = mock(EmailSenderService.class);
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        UserNotificationSettingsService userNotificationSettingsService = mock(UserNotificationSettingsService.class);
        SendEmailFeedbackTask sendEmailFeedbackTask = new SendEmailFeedbackTask(Map.of("test@yandex.ru", List.of("AUTOMORDA_OFF"), "test@yandex-team.ru", List.of()), Map.of(),
                emailSenderService,
                userNotificationSettingsService);

        WebmasterHostId hostId = WebmasterHostId.createNoLowerCase(WebmasterHostId.Schema.HTTP, "test", 80);
        SendFeedbackEMailTaskData sendFeedbackEMailTaskData = new SendFeedbackEMailTaskData(hostId, 1232L, "test", null, FeedbackType.AUTOMORDA_OFF);
        Task.Result run = sendEmailFeedbackTask.run(sendFeedbackEMailTaskData);
        Assert.assertEquals("Task result", run.getTaskResult(), TaskResult.SUCCESS);
        verify(emailSenderService, times(2)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }
}
