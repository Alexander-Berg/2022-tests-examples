package ru.yandex.realty.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface SelenoidService {

    @GET("/download/{session}/{fileName}")
    Call<ResponseBody> downloadFileFromContainer(@Path("session") String session,
                                                 @Path("fileName") String fileName);

}
