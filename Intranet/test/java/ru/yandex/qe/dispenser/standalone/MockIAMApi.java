package ru.yandex.qe.dispenser.standalone;

import ru.yandex.qe.dispenser.ws.iam.IAMApi;
import ru.yandex.qe.dispenser.ws.iam.IAMTokenRequest;
import ru.yandex.qe.dispenser.ws.iam.IAMTokenResponse;

public class MockIAMApi implements IAMApi {
    @Override
    public IAMTokenResponse getToken(IAMTokenRequest request) {
        return new IAMTokenResponse("Test-IAM-token");
    }
}
