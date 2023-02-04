package ru.yandex.realty.api;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.yandex.realty.beans.juridical.JuridicalUserBody;

/**
 * @author kantemirov
 */
public interface Realty3ApiService {

    @Headers("Accept: application/json")
    @GET("offer/{offerId}")
    Call<JsonObject> offerId(@Path("offerId") String offerId);

    @Headers({"Accept: application/json", "X-Authorization: Vertis swagger"})
    @GET("/2.0/user/uid:{uid}/offers/byIds?includeFeature=PRODUCTS")
    Call<JsonObject> price(@Path("uid") String uid, @Query("offerId") String offerId);

    @Headers({"Accept: application/json", "X-Authorization: Vertis swagger"})
    @PUT("/2.0/user/uid:{uid}")
    Call<JsonObject> createUser(@Path("uid") String uid, @Body JuridicalUserBody body);

    @Headers({"Accept: application/json", "X-Authorization: Vertis swagger"})
    @GET("/2.0/subscriptions/promo/user/uid:{uid}")
    Call<JsonObject> subscriptions(@Path("uid") String uid);

}