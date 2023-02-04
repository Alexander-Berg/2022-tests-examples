package ru.auto.tests.desktop.api;

import retrofit2.Call;
import retrofit2.http.POST;
import ru.auto.tests.desktop.beans.tus.CreateAccountResponse;

public interface TusApiService {

    @POST("/1/create_account/portal/")
    Call<CreateAccountResponse> createAccount();

}
