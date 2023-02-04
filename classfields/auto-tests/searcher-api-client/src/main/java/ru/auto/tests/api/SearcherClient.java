package ru.auto.tests.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;
import ru.auto.tests.api.beans.AutoruBreadcrumbs;

import java.util.Map;

public interface SearcherClient {

    /**
     * Ручка хлебных крошек, с параметрами
     */
    @GET("autoruBreadcrumbs")
    Call<AutoruBreadcrumbs> autoruBreadcrumbs(@QueryMap Map<String, String> options);

    /**
     * Ручка хлебных крошек, без параметров
     */
    @GET("autoruBreadcrumbs")
    Call<AutoruBreadcrumbs> autoruBreadcrumbs();

}
