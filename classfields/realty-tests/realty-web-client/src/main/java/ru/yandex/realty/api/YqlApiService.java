package ru.yandex.realty.api;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * @author kantemirov
 */
public interface YqlApiService {

    @Headers({"Accept: application/json", "Content-Type: application/json"})
    @POST("/api/v2/operations")
    Call<JsonObject> yqlOperationsRun(@Header("Authorization") String authorization, @Body JsonObject body);

    @Headers({"Accept: application/json", "Content-Type: application/json"})
    @GET("/api/v2/operations/{id}/results?filters=DATA")
    Call<JsonObject> yqlOperationsData(@Header("Authorization") String authorization, @Path("id") String id);
}