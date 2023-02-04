package ru.yandex.arenda.api.service;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * @author kantemirov
 */
public interface Realty3RentApiService {

    String ACCEPT_APPLICATION_JSON = "Accept: application/json";
    String X_AUTHORIZATION = "X-Authorization: Vertis swagger";
    String CONTENT_TYPE = "Content-Type: application/json";
    //тут админский uid
    String X_UID = "X-UID: 4026826176";

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION})
    @GET("/2.0/rent/user/uid:{uid}/")
    Call<JsonObject> getUserByUid(@Path("uid") String uid);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION})
    @PATCH("/2.0/rent/user/uid:{uid}/")
    Call<JsonObject> patchUserByUid(@Path("uid") String uid, @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION})
    @GET("/2.0/rent/user/uid:{uid}/flats")
    Call<JsonObject> getUserFlats(@Path("uid") String uid);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION})
    @PUT("/2.0/rent/user/uid:{uid}/flats/draft")
    Call<JsonObject> putFlatDraft(@Path("uid") String uid, @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION})
    @POST("/2.0/rent/user/uid:{uid}/flats/draft/confirmation-code/request")
    Call<JsonObject> confirmationCodeRequest(@Path("uid") String uid);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION})
    @POST("/2.0/rent/user/uid:{uid}/flats/draft/confirmation-code/submit")
    Call<JsonObject> confirmationCodeSubmit(@Path("uid") String uid, @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @POST("/2.0/rent/moderation/flats")
    Call<JsonObject> postModerationFlats(@Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @POST("/2.0/rent/moderation/flats/{flatId}/assign-to-user")
    Call<JsonObject> postModerationFlatsAssignToUser(@Path("flatId") String flatId, @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @POST("/2.0/rent/moderation/flats/{flatId}/unassign-user")
    Call<JsonObject> postModerationFlatsUnAssignUser(@Path("flatId") String flatId, @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @POST("/2.0/rent/moderation/flats/{flatId}/rentContracts")
    Call<JsonObject> postModerationFlatsContract(@Path("flatId") String flatId, @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID, CONTENT_TYPE})
    @POST("/2.0/rent/moderation/flats/{flatId}/questionnaire")
    Call<JsonObject> postModerationFlatsQuestionnaire(@Path("flatId") String flatId, @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @POST("/2.0/rent/moderation/flats/{flatId}/house-services/settings/update-status")
    Call<JsonObject> postHouseServices(@Path("flatId") String flatId, @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @DELETE("/2.0/rent/moderation/flats/{flatId}")
    Call<JsonObject> deleteFlat(@Path("flatId") String flatId);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @DELETE("/2.0/rent/moderation/users/{userId}")
    Call<JsonObject> deleteUser(@Path("userId") String userId);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @POST("/2.0/rent/moderation/flats/{flatId}/contracts/{contractId}/update-status")
    Call<JsonObject> flatContractUpdateStatus(@Path("flatId") String flatId, @Path("contractId") String contractId,
                                              @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @POST("/2.0/rent/moderation/flats/{flatId}/owner-requests/{ownerRequestId}/update-status")
    Call<JsonObject> postFlatOwnerRequestIdUpdateStatus(@Path("flatId") String flatId,
                                              @Path("ownerRequestId") String ownerRequestId, @Body JsonObject body);

    @Headers({ACCEPT_APPLICATION_JSON, X_AUTHORIZATION, X_UID})
    @GET("/2.0/rent/moderation/flats/{flatId}/owner-requests")
    Call<JsonObject> getFlatOwnerRequests(@Path("flatId") String flatId);
}
