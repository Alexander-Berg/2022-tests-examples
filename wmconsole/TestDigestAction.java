package ru.yandex.webmaster3.worker.http.digest;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.webmaster3.core.WebmasterException;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.Action;
import ru.yandex.webmaster3.core.http.ActionRequest;
import ru.yandex.webmaster3.core.http.ActionResponse;
import ru.yandex.webmaster3.core.http.ActionStatus;
import ru.yandex.webmaster3.core.http.RequestQueryProperty;
import ru.yandex.webmaster3.core.notification.LanguageEnum;
import ru.yandex.webmaster3.storage.user.notification.NotificationType;
import ru.yandex.webmaster3.worker.digest.DigestDevSenderService;

/**
 * @author avhaliullin
 */
@AllArgsConstructor(onConstructor_ = @Autowired)
public class TestDigestAction extends Action<TestDigestAction.Req, TestDigestAction.Res> {
    private final DigestDevSenderService digestDevSenderService;

    @Override
    public Res process(Req request) throws WebmasterException {
        int sent = 0;
        for (WebmasterHostId hostId : request.hostId) {
            if (hostId != null) {
                if (digestDevSenderService.sendDigest(request.email, request.lang, request.notificationType, request.digestDate, hostId)) {
                    sent++;
                }
            }
        }

        return new Res(sent, request.hostId.size() - sent);
    }

    public static class Req implements ActionRequest {
        private List<WebmasterHostId> hostId;
        private LocalDate digestDate;
        private NotificationType notificationType = NotificationType.DIGEST;
        private String email;
        private LanguageEnum lang;

        @RequestQueryProperty(required = true)
        public void setHostId(List<WebmasterHostId> hostId) {
            this.hostId = hostId;
        }

        @RequestQueryProperty(required = true)
        public void setDigestDate(LocalDate digestDate) {
            this.digestDate = digestDate;
        }

        @RequestQueryProperty(required = true)
        public void setEmail(String email) {
            this.email = email;
        }

        @RequestQueryProperty
        public void setLang(LanguageEnum lang) {
            this.lang = lang;
        }

        @RequestQueryProperty
        public void setNotificationType(NotificationType notificationType) {
            this.notificationType = notificationType;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Res implements ActionResponse {
        private final int sent;
        private final int errors;

        @Override
        public ActionStatus getRequestStatus() {
            return ActionStatus.SUCCESS;
        }
    }
}
