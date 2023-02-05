package com.yandex.mail.fakeserver;

import androidx.annotation.NonNull;
import okhttp3.mockwebserver.RecordedRequest;

public interface CustomResponseRule extends MockWebServerResponseRule {

    boolean match(@NonNull RecordedRequest request);
}
