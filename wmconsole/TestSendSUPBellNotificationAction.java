package ru.yandex.webmaster3.worker.notifications;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.webmaster3.core.WebmasterException;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.Action;
import ru.yandex.webmaster3.core.http.ActionRequest;
import ru.yandex.webmaster3.core.http.ActionResponse;
import ru.yandex.webmaster3.core.http.ReadAction;
import ru.yandex.webmaster3.core.http.RequestQueryProperty;
import ru.yandex.webmaster3.core.sup.SupIntegrationService;
import ru.yandex.webmaster3.core.sup.model.BellQuery;
import ru.yandex.webmaster3.worker.notifications.auto.bell.model.SimpleBellMeta;

/**
 * ishalaru
 * 05.08.2020
 **/
@ReadAction
@Component("/test/sendBellnotification")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TestSendSUPBellNotificationAction extends Action<TestSendSUPBellNotificationAction.Request, TestSendSUPBellNotificationAction.Response> {
    private final SupIntegrationService supIntegrationService;

    @Override
    public Response process(Request request) throws WebmasterException {
        BellQuery.BellNotification.BellMeta bellMeta = SimpleBellMeta.create(request.link, request.hostId.toStringId());
        final BellQuery.BellNotification bellNotification = new BellQuery.BellNotification("ya_webmaster", request.type, "webmaster", bellMeta);
        supIntegrationService.sendBell(List.of(request.uid), bellNotification);
        return new Response();
    }

    @RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
    public static class Request implements ActionRequest {
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        WebmasterHostId hostId;

        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        Long uid;

        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        String type;
        @Setter(onMethod = @__({@RequestQueryProperty(required = true)}))
        String link;
    }

    public static class Response implements ActionResponse.NormalResponse {
    }
}
