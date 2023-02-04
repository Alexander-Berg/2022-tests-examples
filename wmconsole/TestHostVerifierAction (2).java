package ru.yandex.webmaster3.worker.http;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.host.verification.IUserHostVerifier;
import ru.yandex.webmaster3.core.host.verification.VerificationCausedBy;
import ru.yandex.webmaster3.core.host.verification.VerificationFailInfo;
import ru.yandex.webmaster3.core.host.verification.VerificationType;
import ru.yandex.webmaster3.core.http.Action;
import ru.yandex.webmaster3.core.http.ActionRequest;
import ru.yandex.webmaster3.core.http.ActionResponse;
import ru.yandex.webmaster3.core.http.ReadAction;
import ru.yandex.webmaster3.core.http.RequestQueryProperty;
import ru.yandex.webmaster3.core.util.IdUtils;

/**
 * ishalaru
 * 28.07.2020
 **/
@ReadAction
@Component("/host/testVerifier")
public final class TestHostVerifierAction extends Action<TestHostVerifierAction.Req, TestHostVerifierAction.Res> {
    private static final Logger log = LoggerFactory.getLogger(TestHostVerifierAction.class);

    private Map<VerificationType, IUserHostVerifier> hostVerifiersMap;


    @Override
    public Res process(Req req) {
        IUserHostVerifier verifier = hostVerifiersMap.get(req.verificationType);
        WebmasterHostId hostId = IdUtils.stringToHostId(req.hostId);
        Optional<VerificationFailInfo> failInfo = verifier.verify(req.userId, hostId, null, req.verificationUin, req.verificationCausedBy);
        log.info("Verification fail info: {}", failInfo);
        return new Res();
    }

    @Required
    public void setHostVerifiersMap(Map<VerificationType, IUserHostVerifier> hostVerifiersMap) {
        this.hostVerifiersMap = hostVerifiersMap;
    }

    public static class Req implements ActionRequest {
        private String hostId;
        private long userId;
        private long verificationUin;
        private VerificationType verificationType;
        private VerificationCausedBy verificationCausedBy;

        @RequestQueryProperty(required = true)
        public void setHostId(String hostId) {
            this.hostId = hostId;
        }

        @RequestQueryProperty(required = false)
        public void setUserId(long userId) {
            this.userId = userId;
        }

        @RequestQueryProperty(required = false)
        public void setVerificationUin(long verificationUin) {
            this.verificationUin = verificationUin;
        }

        @RequestQueryProperty(required = true)
        public void setVerificationType(VerificationType verificationType) {
            this.verificationType = verificationType;
        }

        @RequestQueryProperty(required = true)
        public void setVerificationCausedBy(VerificationCausedBy verificationCausedBy) {
            this.verificationCausedBy = verificationCausedBy;
        }
    }

    public static class Res implements ActionResponse.NormalResponse {
    }
}