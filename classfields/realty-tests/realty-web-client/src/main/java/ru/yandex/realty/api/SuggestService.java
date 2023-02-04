package ru.yandex.realty.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.yandex.realty.beans.SuggestResponse;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface SuggestService {

    @GET("newbuildingDeveloperSuggest")
    Call<SuggestResponse> developerList(@Query("text") String search, @Query("rgid") String rgid);

    @GET("newbuildingNameSuggest")
    Call<SuggestResponse> nameList(@Query("text") String search, @Query("rgid") String rgid);

}
