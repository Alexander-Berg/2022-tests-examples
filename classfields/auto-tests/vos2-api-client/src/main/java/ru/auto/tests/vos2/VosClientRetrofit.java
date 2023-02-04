package ru.auto.tests.vos2;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import ru.auto.tests.vos2.models.Response;


public interface VosClientRetrofit {

    @DELETE("/api/v1/offer/{category}/{offerId}")
    Call<Response> deleteOffer(@Path("category") String category, @Path("offerId") String offerId);
}
