package ru.auto.tests.passport.client;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.auto.tests.passport.beans.Code;
import ru.auto.tests.passport.beans.ConfirmAndBindSecureSubmitResp;
import ru.auto.tests.passport.beans.CreateAccountResp;
import ru.auto.tests.passport.beans.Track;

public interface PassportApiService {

    @Headers({"Ya-Client-Host: passport.yandex.ru",
            "Accept: */*",
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8",
            "Ya-Consumer-Client-Scheme: https"})
    @POST("track/")
    Call<Track> track(@Header("Ya-Consumer-Client-Ip") String consumerClientIp,
                      @Query("testReqId") String testReqId,
                      @Query("consumer") String consumer);

    @Headers("Accept: */*")
    @GET("bundle/test/track/")
    Call<Track> rereadTrack(@Header("Ya-Consumer-Client-Ip") String consumerClientIp,
                            @Query("testReqId") String testReqId,
                            @Query("consumer") String consumer,
                            @Query("track_id") String trackId);

    @FormUrlEncoded
    @Headers({"Ya-Client-Host: passport.yandex.ru",
            "Accept: */*",
            "Ya-Consumer-Client-Scheme: https",
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8"})
    @POST("bundle/phone/confirm/submit/")
    Call<ResponseBody> sendConfirmationCode(@Header("Ya-Consumer-Client-Ip") String consumerClientIp,
                                            @Query("testReqId") String testReqId,
                                            @Query("consumer") String consumer,
                                            @Field("display_language") String language,
                                            @Field("number") String number,
                                            @Field("track_id") String trackId);

    @FormUrlEncoded
    @Headers({"Ya-Client-Host: passport.yandex.ru",
            "Accept: */*",
            "Ya-Consumer-Client-Scheme: https",
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8"})
    @POST("bundle/phone/confirm/commit/")
    Call<ResponseBody> completeConfirmation(@Header("Ya-Consumer-Client-Ip") String consumerClientIp,
                                            @Query("testReqId") String testReqId,
                                            @Query("consumer") String consumer,
                                            @Field("code") String code,
                                            @Field("track_id") String trackId);

    @FormUrlEncoded
    @Headers({"Ya-Client-Host: passport.yandex.ru",
            "Accept: */*",
            "Ya-Client-User-Agent: Mozilla/5.0",
            "Ya-Consumer-Client-Scheme: https",
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8"})
    @POST("bundle/phone/confirm_and_bind_secure/submit/")
    Call<ConfirmAndBindSecureSubmitResp> confirmAndBindSecureSubmit(@Header("Ya-Consumer-Client-Ip") String consumerClientIp,
                                                                    @Query("testReqId") String testReqId,
                                                                    @Query("consumer") String consumer,
                                                                    @Field("display_language") String displayLanguage,
                                                                    @Field("uid") String uid,
                                                                    @Field("number") String number);

    @FormUrlEncoded
    @Headers({"Ya-Client-Host: passport.yandex.ru",
            "Accept: */*",
            "Ya-Client-User-Agent: Mozilla/5.0",
            "Ya-Consumer-Client-Scheme: https",
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8"})
    @POST("bundle/phone/confirm_and_bind_secure/commit/")
    Call<ResponseBody> confirmAndBindSecureCommit(@Header("Ya-Consumer-Client-Ip") String consumerClientIp,
                                                  @Query("testReqId") String testReqId,
                                                  @Query("consumer") String consumer,
                                                  @Field("track_id") String trackId,
                                                  @Field("code") String code,
                                                  @Field("password") String password);

    @FormUrlEncoded
    @Headers({"Ya-Client-User-Agent: Mozilla/5.0",
            "Accept: */*",
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8"})
    @POST("account/register/alternative/")
    Call<CreateAccountResp> registerWithPhone(@Header("Ya-Consumer-Client-Ip") String consumerClientIp,
                                              @Query("testReqId") String testReqId,
                                              @Query("consumer") String consumer,
                                              @Field("validation_method") String validationMethod,
                                              @Field("login") String login,
                                              @Field("password") String password,
                                              @Field("track_id") String trackId,
                                              @Field("firstname") String firstName,
                                              @Field("lastname") String lastName,
                                              @Field("language") String language,
                                              @Field("country") String country,
                                              @Field("eula_accepted") String eulaAccepted);

    @FormUrlEncoded
    @Headers({"Ya-Client-User-Agent: Mozilla/5.0",
            "Accept: */*",
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8"})
    @POST("bundle/account/register/")
    Call<CreateAccountResp> registerWithoutPhone(@Header("Ya-Consumer-Client-Ip") String consumerClientIp,
                                                 @Query("testReqId") String testReqId,
                                                 @Query("consumer") String consumer,
                                                 @Field("login") String login,
                                                 @Field("password") String password,
                                                 @Field("firstname") String firstName,
                                                 @Field("lastname") String lastName,
                                                 @Field("language") String language,
                                                 @Field("country") String country);

    @Headers({"Accept: */*"})
    @GET("bundle/test/get_phones/")
    Call<Code> getPhones(@Query("testReqId") String testReqId,
                         @Query("consumer") String consumer,
                         @Query("uid") String uid);


    @Headers({"Ya-Client-User-Agent: Mozilla/5.0",
            "Accept: */*"})
    @DELETE("bundle/account/{uid}/")
    Call<ResponseBody> delete(@Path("uid") String uid,
                              @Header("Ya-Consumer-Client-Ip") String consumerClientIp,
                              @Header("Ya-Client-Host") String clientHost,
                              @Query("testReqId") String testReqId,
                              @Query("consumer") String consumer);
}
