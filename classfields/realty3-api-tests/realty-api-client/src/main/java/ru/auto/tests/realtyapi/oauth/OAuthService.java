package ru.auto.tests.realtyapi.oauth;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface OAuthService {

     @POST("token")
     @FormUrlEncoded
     Call<TokenResponse> token(@Header("Authorization") String auth,
                               @Query("user_ip") String ip,
                               @Field("grant_type") String grantType,
                               @Field("username") String userName,
                               @Field("password") String password,
                               @Field("client_id") String clientId,
                               @Field("client_secret") String clientSecret);


}
