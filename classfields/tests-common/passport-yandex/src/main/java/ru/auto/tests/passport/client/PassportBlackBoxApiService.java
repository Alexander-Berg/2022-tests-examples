package ru.auto.tests.passport.client;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import ru.auto.tests.passport.beans.Blackbox;

public interface PassportBlackBoxApiService {

    @FormUrlEncoded
    @Headers({"Accept: */*",
            "X-Forwarded-For: 77.88.27.152",
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8"})
    @POST("blackbox?emails=getall&regname=yes&authid=1&aliases=all&host=yandex.ru")
    Call<Blackbox> blackbox(@Query("testReqId") String testReqId,
                            @Field("method") String method,
                            @Field("userip") String userIp,
                            @Field("format") String format,
                            @Field("dbfields") String dbfields,
                            @Field("attributes") String attributes,
                            @Field("uid") String uid);
}
