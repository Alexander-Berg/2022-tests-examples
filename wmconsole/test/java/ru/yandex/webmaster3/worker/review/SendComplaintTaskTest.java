package ru.yandex.webmaster3.worker.review;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import ru.yandex.webmaster3.core.blackbox.UserWithLogin;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.notification.LanguageEnum;
import ru.yandex.webmaster3.core.review.SendReviewComplaintEMailTaskData;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.W3Collectors;
import ru.yandex.webmaster3.storage.notifications.service.EmailSenderService;
import ru.yandex.webmaster3.storage.user.UserPersonalInfo;
import ru.yandex.webmaster3.storage.user.service.UserPersonalInfoService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author: ishalaru
 * DATE: 22.07.2019
 */
public class SendComplaintTaskTest {


    @Test
    public void sendMailTest() throws Exception {

        final var builder = SendReviewComplaintEMailTaskData.builder();
        final DateTime now = DateTime.now();
        final WebmasterHostId hostId = WebmasterHostId.createNoLowerCase(WebmasterHostId.Schema.HTTP, "yandex.ru", 80);
        var value = builder.complaintText("ComplaintText")
                .complaintTime(now)
                .hostId(hostId)
                .reviewId("100-1")
                .reviewText("abuse")
                .reviewTime(now.minusDays(1))
                .reviewUserId(100L)
                .userId(200L)
                .build();

        EmailSenderService emailSenderService = mock(EmailSenderService.class);
        UserPersonalInfoService userPersonalInfoService = mock(UserPersonalInfoService.class);
        when(userPersonalInfoService.getUsersPersonalInfos(any())).thenReturn(initUserInfo(List.of(100L, 200L)));
        ArgumentCaptor<String> address = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        when(emailSenderService.sendEmail(address.capture(), anyString(), subject.capture(), body.capture())).thenReturn(true);
        final SendComplaintTask sendComplaintTask = new SendComplaintTask(emailSenderService, userPersonalInfoService, List.of("test@yandex.ru"));
        sendComplaintTask.run(value);
        Assert.assertEquals("How many times message was sended", address.getAllValues().size(), 1);
        Assert.assertEquals("Addresses for sending", address.getValue(), "test@yandex.ru");
        Assert.assertTrue("Subject contains host", subject.getValue().contains(IdUtils.hostIdToUrl(hostId)));
        Assert.assertTrue("Subject contains reviewId", body.getValue().contains("100-1"));
    }

    private Map<Long, UserPersonalInfo> initUserInfo(Collection<Long> userId) {
        //Map<Long,UserPersonalInfo>
        return userId.stream().map(e ->
                new UserPersonalInfo(e, String.valueOf(userId), "FIO:" + String.valueOf(userId), LanguageEnum.DEFAULT_EMAIL_LANGUAGE)
        ).collect(Collectors.toMap(UserWithLogin::getUserId, e -> e, W3Collectors.throwingMerger()));
    }
}