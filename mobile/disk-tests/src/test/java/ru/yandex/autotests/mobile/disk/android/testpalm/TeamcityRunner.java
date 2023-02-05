package ru.yandex.autotests.mobile.disk.android.testpalm;

import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TeamcityRunner {
    private static final String ACCEPTANCE = "MobileNew_Monorepo_Disk_Android_MobileDiskClientAndroidIt_DiskAndroidAutotestsReleaseAcceptance";
    private static final String BUSINESS_LOGIC = "MobileNew_Monorepo_Disk_Android_MobileDiskClientAndroidIt_DiskAndroidAutotestsReleaseBusinessLogic";
    private static final String REGRESSION = "MobileNew_Monorepo_Disk_Android_MobileDiskClientAndroidIt_DiskAndroidAutotestsReleaseRegression";
    private static final String FULL_REGRESSION = "MobileNew_Monorepo_Disk_Android_MobileDiskClientAndroidIt_DiskAndroidAutotestsReleaseRegressionFull";
    private static final String[] LEVELS = {ACCEPTANCE, BUSINESS_LOGIC, REGRESSION};

    public static void main(String[] args) {
        String teamcityOAuth = args[0];
        String buildNumber = args[1];
        for (String level : LEVELS) {
            doPost(teamcityOAuth, level, buildNumber);
        }
    }

    private static void doPost(String teamcityOAuth, String level, String buildNumber) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        String data = "<build><buildType id=\"" + level + "\"/><properties><property name=\"apk.build.number\" value=\"" + buildNumber + "\"/></properties></build>";
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/xml"), data);
        Request request = new Request.Builder()
                .url("http://teamcity.yandex-team.ru/app/rest/buildQueue")
                .addHeader("Authorization", "Bearer " + teamcityOAuth)
                .post(requestBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String bodyString = response.body().string();
            if (response.isSuccessful()) {
                System.out.println("Request succeed: " + bodyString);
            } else {
                System.out.println("Request failed: " + bodyString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
