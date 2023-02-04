package ru.auto.tests.passport.client;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import ru.auto.tests.passport.beans.Captcha;

public interface CaptchaApiService {

    @Headers({"Accept: */*"})
    @GET("answer")
    Call<Captcha> captcha(@Query("testReqId") String testReqId,
                          @Query("key") String method);
}
