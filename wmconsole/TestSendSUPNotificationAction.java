package ru.yandex.webmaster3.worker.notifications;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.webmaster3.core.WebmasterException;
import ru.yandex.webmaster3.core.http.Action;
import ru.yandex.webmaster3.core.http.ActionRequest;
import ru.yandex.webmaster3.core.http.ReadAction;
import ru.yandex.webmaster3.core.http.RequestQueryProperty;
import ru.yandex.webmaster3.core.sup.SupIntegrationService;
import ru.yandex.webmaster3.storage.turbo.service.settings.SetTurboSettingsResponse;

/**
 * ishalaru
 * 19.03.2020
 **/
@ReadAction
@Component("/test/sendSUPnotification")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TestSendSUPNotificationAction extends Action<TestSendSUPNotificationAction.Request, TestSendSUPNotificationAction.Response> {

    private final SupIntegrationService supIntegrationService;

    @Override
    public Response process(Request request) throws WebmasterException {
        SupIntegrationService.PushData pushData = new SupIntegrationService.PushData(request.pushId,
                request.title,
                "Я.Вебмастер",
                request.shortTitle,
                request.body,
                request.link,
                List.of(request.uid));
        supIntegrationService.send(pushData);
        return null;
    }

    @RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
    public static class Request implements ActionRequest {
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        String pushId;
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        String title;
        @Setter(onMethod = @__({@RequestQueryProperty(required = false)}))
        String shortTitle;

        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        String body;
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        String link;
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        Long uid;
    }

    public static class Response extends SetTurboSettingsResponse.NormalResponse {
    }
}
