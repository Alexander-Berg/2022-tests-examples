package ru.yandex.webmaster3.worker.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.Action;
import ru.yandex.webmaster3.core.http.ActionRequest;
import ru.yandex.webmaster3.core.http.ActionResponse;
import ru.yandex.webmaster3.core.http.ReadAction;
import ru.yandex.webmaster3.core.http.RequestQueryProperty;
import ru.yandex.webmaster3.core.notification.LanguageEnum;
import ru.yandex.webmaster3.storage.notifications.service.EmailSenderService;
import ru.yandex.webmaster3.storage.notifications.service.UserNotificationSettingsService;
import ru.yandex.webmaster3.storage.postpone.PostWelcomeEmailData;
import ru.yandex.webmaster3.worker.notifications.services.PostWelcomeMessageBodyBuilderService;

/**
 * ishalaru
 * 25.03.2020
 **/
@Slf4j
@ReadAction
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Component("/test/postWelcomeNotification")
public class TestSendPostWelcomeEmail extends Action<TestSendPostWelcomeEmail.Request, TestSendPostWelcomeEmail.Response> {
    private final PostWelcomeMessageBodyBuilderService postWelcomeMessageBodyBuilderService;
    private final UserNotificationSettingsService userNotificationSettingsService;
    private final EmailSenderService emailSenderService;

    @Override
    public Response process(Request request) {
        String userEmail = userNotificationSettingsService.getUserEmailOrDefaultIfEmpty(request.getUserId());
        if (userEmail == null) {
            log.error("User:{}, don't have email", request.getUserId());
            return null;
        }
        PostWelcomeEmailData postWelcomeEmailData = new PostWelcomeEmailData(request.getUserId(), request.hostId, LanguageEnum.RU);
        emailSenderService.sendEmail(userEmail, "",
                postWelcomeMessageBodyBuilderService.buildSubject(postWelcomeEmailData, request.shortEmail),
                postWelcomeMessageBodyBuilderService.buildBody(postWelcomeEmailData, request.shortEmail, request.turboEcommerse));

        return new Response();
    }

    @Getter
    @RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
    public static class Request implements ActionRequest {
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        WebmasterHostId hostId;
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        long userId;
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        boolean shortEmail;
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        boolean turboEcommerse;
    }

    public static class Response implements ActionResponse.NormalResponse {
    }
}
