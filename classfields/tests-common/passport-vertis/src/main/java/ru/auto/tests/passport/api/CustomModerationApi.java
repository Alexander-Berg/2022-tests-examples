package ru.auto.tests.passport.api;

import retrofit2.Call;
import retrofit2.http.GET;
import ru.auto.test.passport.model.SmsLogRecord;

import java.util.List;

public interface CustomModerationApi {

    // TODO: Fix this model in passport API and remove this custom API declaration
    @GET("{service}/moderation/sms-log")
    Call<List<SmsLogRecord>> getSmsLogs(
            @retrofit2.http.Path("service") String service,
            @retrofit2.http.Query("phone") String phone,
            @retrofit2.http.Query("count") Integer count
    );
}
