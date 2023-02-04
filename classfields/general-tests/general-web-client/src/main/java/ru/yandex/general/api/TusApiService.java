package ru.yandex.general.api;

import retrofit2.Call;
import retrofit2.http.POST;
import ru.yandex.general.beans.tus.CreateAccountResponse;

public interface TusApiService {

    @POST("/1/create_account/portal/")
    Call<CreateAccountResponse> createAccount();

}
