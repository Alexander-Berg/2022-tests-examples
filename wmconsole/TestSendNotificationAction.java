package ru.yandex.webmaster3.worker.notifications;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.webmaster3.core.WebmasterException;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.Action;
import ru.yandex.webmaster3.core.http.ActionRequest;
import ru.yandex.webmaster3.core.http.ActionResponse;
import ru.yandex.webmaster3.core.http.ReadAction;
import ru.yandex.webmaster3.core.http.RequestQueryProperty;
import ru.yandex.webmaster3.core.metrika.counters.CounterRequestTypeEnum;
import ru.yandex.webmaster3.core.sitestructure.SearchUrlStatusEnum;
import ru.yandex.webmaster3.core.turbo.model.feed.TurboFeedType;
import ru.yandex.webmaster3.core.util.json.JsonMapping;
import ru.yandex.webmaster3.storage.abt.model.ExperimentInfo;
import ru.yandex.webmaster3.storage.events.data.WMCEvent;
import ru.yandex.webmaster3.storage.events.data.WMCEventType;
import ru.yandex.webmaster3.storage.events.data.events.UserHostMessageEvent;
import ru.yandex.webmaster3.storage.events.service.WMCEventsObservingProcessingService;
import ru.yandex.webmaster3.storage.user.message.MessageTypeEnum;
import ru.yandex.webmaster3.storage.user.message.content.MessageContent;
import ru.yandex.webmaster3.storage.user.notification.NotificationType;

/**
 * @author aherman
 */
@ReadAction
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TestSendNotificationAction extends Action<TestSendNotificationAction.Request, TestSendNotificationAction.Response> {
    private static final Logger log = LoggerFactory.getLogger(TestSendNotificationAction.class);

    private final WMCEventsObservingProcessingService autoNotificationEmailObserver;
    private final WMCEventsObservingProcessingService autoNotificationServiceObserver;
    private final WMCEventsObservingProcessingService autoNotificationSupObserver;
    private final NotificationTestService notificationTestService;

    @Override
    public Response process(Request request) throws WebmasterException {
        log.info("Request: {}", request);
        if (request.sendExamples) {
            createDifferentTypeOfMessages(request.userId, request.hostId);
            return new Response();
        }

        ExperimentInfo experimentInfo = Strings.isNullOrEmpty(request.experimentInfo) ? null :
                JsonMapping.readValue(request.experimentInfo, ExperimentInfo.class);
        WMCEvent event = notificationTestService.getEvent(
                request.userId, request.hostId, request.notificationType, request.messageType,
                request.problemType, request.searchStatus, request.httpCode,
                request.feedType, request.metrikaReqType, request.data, experimentInfo, request.repeated);

        if (event == null) {
            return new Response();
        }
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }
        try {
            autoNotificationSupObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }
        try {
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);

        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }
        return new Response();
    }


    public void createDifferentTypeOfMessages(Long userId, WebmasterHostId hostId) {
        String data = "{\"messageType\":\"RIVAL_UPDATE\",\"type\":\"TYPE_ONE\"}";
        String experimentInfo = "{\"experiment\":\"IKS_UPDATE_EMAIL\",\"group\":\"TYPE_1\"}";
        WMCEvent event = notificationTestService.getEvent(
                userId, hostId, NotificationType.IKS_UPDATE,
                SiteProblemTypeEnum.CONNECT_FAILED, SearchUrlStatusEnum.HOST_ERROR, 400,
                TurboFeedType.AUTO_MORDA, CounterRequestTypeEnum.CREATE, data, JsonMapping.readValue(experimentInfo, ExperimentInfo.class), false);
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }

        event = notificationTestService.getEvent(
                userId, hostId, NotificationType.SITE_PROBLEM_FATAL,
                SiteProblemTypeEnum.CONNECT_FAILED, SearchUrlStatusEnum.HOST_ERROR, 400,
                TurboFeedType.AUTO_MORDA, CounterRequestTypeEnum.CREATE, data, JsonMapping.readValue(experimentInfo, ExperimentInfo.class), false);
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }
        event = notificationTestService.getEvent(
                userId, hostId, NotificationType.SITE_PROBLEM_CRITICAL,
                SiteProblemTypeEnum.DOMAIN_EXPIRES, SearchUrlStatusEnum.HOST_ERROR, 400,
                TurboFeedType.AUTO_MORDA, CounterRequestTypeEnum.CREATE, data, JsonMapping.readValue(experimentInfo, ExperimentInfo.class), false);
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }

        event = notificationTestService.getEvent(
                userId, hostId, NotificationType.SITE_PROBLEM_POSSIBLE,
                SiteProblemTypeEnum.MAIN_MIRROR_IS_NOT_HTTPS, SearchUrlStatusEnum.HOST_ERROR, 400,
                TurboFeedType.AUTO_MORDA, CounterRequestTypeEnum.CREATE, data, JsonMapping.readValue(experimentInfo, ExperimentInfo.class), false);
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }

        event = notificationTestService.getEvent(userId, hostId,
                NotificationType.NEW_REVIEW_AVAILABLE,
                SiteProblemTypeEnum.SLOW_AVG_RESPONSE_WITH_EXAMPLES,
                SearchUrlStatusEnum.BAD_MIME_TYPE, 400,
                TurboFeedType.AUTO_MORDA, CounterRequestTypeEnum.CONFIRM, JsonMapping.readValue(experimentInfo, ExperimentInfo.class));
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }
        event = notificationTestService.getEvent(userId, hostId,
                NotificationType.URL_SEARCH_STATUS_CHANGE,
                SiteProblemTypeEnum.SLOW_AVG_RESPONSE_WITH_EXAMPLES,
                SearchUrlStatusEnum.BAD_MIME_TYPE, 400,
                TurboFeedType.AUTO_MORDA, CounterRequestTypeEnum.CONFIRM, JsonMapping.readValue(experimentInfo, ExperimentInfo.class));
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }
        event = notificationTestService.getEvent(userId, hostId,
                NotificationType.SEARCH_BASE_UPDATE,
                SiteProblemTypeEnum.SLOW_AVG_RESPONSE_WITH_EXAMPLES,
                SearchUrlStatusEnum.BAD_MIME_TYPE, 400,
                TurboFeedType.AUTO_MORDA, CounterRequestTypeEnum.CONFIRM, JsonMapping.readValue(experimentInfo, ExperimentInfo.class));
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }
        event = notificationTestService.getEvent(
                userId, hostId, NotificationType.SITE_PROBLEM_RECOMMENDATION,
                SiteProblemTypeEnum.BIG_FAVICON_ABSENT, SearchUrlStatusEnum.HOST_ERROR, 400,
                TurboFeedType.AUTO_MORDA, CounterRequestTypeEnum.CREATE, data, JsonMapping.readValue(experimentInfo, ExperimentInfo.class), false);
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }
        event = WMCEvent.create(new UserHostMessageEvent<>(hostId, userId, new MessageContent.HostAccessDelegatedToNotUser("test"), NotificationType.SITE_PROBLEM_POSSIBLE, false));
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }

        event = WMCEvent.create(new UserHostMessageEvent<>(hostId, userId, new MessageContent.TurboListingsAvailable(hostId), NotificationType.TURBO_ERROR, false));
        try {
            autoNotificationEmailObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
            autoNotificationServiceObserver.getWmcEventsObserverMap().get(WMCEventType.USER_HOST_MESSAGE).observe(event);
        } catch (Exception exp) {
            log.error(exp.getMessage(), exp);
        }

        log.info("Sent messages for {}, {}", userId, hostId);
    }

    public static class Request implements ActionRequest {
        private long userId;
        private WebmasterHostId hostId;
        private NotificationType notificationType = NotificationType.SITE_PROBLEM_CRITICAL;
        @Setter(onMethod_ = @RequestQueryProperty)
        private MessageTypeEnum messageType;
        private SiteProblemTypeEnum problemType;
        private SearchUrlStatusEnum searchStatus;
        private int httpCode = 404;
        private TurboFeedType feedType;
        private CounterRequestTypeEnum metrikaReqType;
        private String data;
        private String experimentInfo;
        private boolean sendExamples;
        @Setter(onMethod_ = @RequestQueryProperty)
        private boolean repeated;

        @RequestQueryProperty(required = true)
        public void setUserId(long userId) {
            this.userId = userId;
        }

        @RequestQueryProperty(required = true)
        public void setHostId(WebmasterHostId hostId) {
            this.hostId = hostId;
        }

        @RequestQueryProperty
        public void setSearchStatus(SearchUrlStatusEnum searchStatus) {
            this.searchStatus = searchStatus;
        }

        @RequestQueryProperty
        public void setNotificationType(NotificationType notificationType) {
            this.notificationType = notificationType;
        }

        @RequestQueryProperty
        public void setProblemType(SiteProblemTypeEnum problemType) {
            this.problemType = problemType;
        }

        @RequestQueryProperty
        public void setHttpCode(int httpCode) {
            this.httpCode = httpCode;
        }

        @RequestQueryProperty
        public void setFeedType(TurboFeedType feedType) {
            this.feedType = feedType;
        }

        @RequestQueryProperty
        public void setMetrikaReqType(CounterRequestTypeEnum metrikaReqType) {
            this.metrikaReqType = metrikaReqType;
        }

        @RequestQueryProperty
        public void setData(String data) {
            this.data = data;
        }

        @RequestQueryProperty
        public void setExperimentInfo(String experimentInfo) {
            this.experimentInfo = experimentInfo;
        }

        @RequestQueryProperty
        public void setSendExamples(boolean sendExamples) {
            this.sendExamples = sendExamples;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("userId", userId)
                    .add("hostId", hostId)
                    .add("notificationType", notificationType)
                    .add("problemType", problemType)
                    .add("searchStatus", searchStatus)
                    .add("httpCode", httpCode)
                    .add("feedType", feedType)
                    .add("metrikaReqType", metrikaReqType)
                    .toString();
        }
    }

    public static class Response implements ActionResponse.NormalResponse {
    }
}
