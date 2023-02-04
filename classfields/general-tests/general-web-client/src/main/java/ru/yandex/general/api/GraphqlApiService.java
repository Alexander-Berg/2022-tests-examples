package ru.yandex.general.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import ru.yandex.general.beans.graphql.Request;
import ru.yandex.general.beans.graphql.Response;

public interface GraphqlApiService {

    @POST("/api/graphql")
    Call<Response> post(@Body Request request);

}
