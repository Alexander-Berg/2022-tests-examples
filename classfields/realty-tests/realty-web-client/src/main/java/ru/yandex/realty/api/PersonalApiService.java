package ru.yandex.realty.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import ru.yandex.realty.beans.favorites.FavoritesResponse;

public interface PersonalApiService {

    @Headers("Accept: application/json")
    @GET("favorites/2.0/realty:offers/uid:{uid}")
    Call<FavoritesResponse> favorites(@Path("uid") String uid);

}
