package com.yandex.mail.fakeserver;

import androidx.annotation.NonNull;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

public interface MockWebServerResponseRule {
    @NonNull
    MockResponse getResponse(@NonNull RecordedRequest request);
}
