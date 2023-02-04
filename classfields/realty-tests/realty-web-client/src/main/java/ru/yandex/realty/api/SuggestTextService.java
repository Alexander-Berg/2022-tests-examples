package ru.yandex.realty.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.yandex.realty.beans.SuggestText;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface SuggestTextService {

    @GET("gate/react-suggest/geo-area")
    Call<SuggestText> suggestText(@Query("text") String search,
                                  @Query("rgid") String rgid,
                                  @Query("type") String type,
                                  @Query("category") String category,
                                  @Query("extendedSearch") String ext);
}
