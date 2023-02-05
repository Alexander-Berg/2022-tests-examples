package ru.yandex.autotests.mobile.disk.android.testpalm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class TicketCreator {
    private static final int COMPONENT = 48383;
    private static final String PARAMETERS_FILE = "parameters.txt";
    private static final String BUILD_STATUS_FILE = "BUILD_STATUS";
    private static final String TRUNK = "trunk";

    public static void main(String[] args) {
        String teamcityBuildId = args[0];
        String suite = args[1];
        String androidVersion = args[2];
        String buildNumber = args[3];
        String branch = args[4];
        String buildVersion = readVersion(PARAMETERS_FILE);
        String stToken = System.getProperty("startrek.token");

        String allure = "https://teamcity.yandex-team.ru/repository/download/mobile_disk_android_autotest_acceptance/"
                + teamcityBuildId + ":id/allure-report.zip!/allure-maven-plugin/index.html";
        boolean trunk = TRUNK.equals(branch);
        String link = doCreateTicket(allure, suite, buildVersion, stToken, androidVersion, buildNumber, trunk);
        createBuildStatusFile(link);
    }

    private static void createBuildStatusFile(String link) {
        try {
            File buildStatus = new File(BUILD_STATUS_FILE);
            if (buildStatus.createNewFile() && link != null) {
                Files.write(Paths.get(BUILD_STATUS_FILE), Collections.singletonList(link), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readVersion(String filepath) {
        File file = new File(filepath);
        try (Scanner reader = new Scanner(file)) {
            return reader.nextLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Return startrek ticket link
     */
    private static String doCreateTicket(
            String allure,
            String suite,
            String buildVersion,
            String stToken,
            String androidVersion,
            String buildNumber,
            boolean trunk
    ) {
        System.out.println("Artifact: " + allure + ", suite: " + suite + ", buildVersion: " +
                buildVersion + ", androidVersion: " + androidVersion + ", buildNumber: " + buildNumber);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        JsonObject json = new JsonObject();
        json.addProperty("queue", "MOBDISKQA");
        String summary = tag(buildVersion) + " " + tag(suite) + " " + tag(androidVersion) + " " + tag(buildNumber) + " "
                + "Разбор упавших тестов " + currentDate();
        json.addProperty("summary", summary);
        json.addProperty("description", allure);
        JsonArray tags = new JsonArray();
        tags.add("RazborAutotestov");
        if (trunk) {
            tags.add(TRUNK);
        }
        json.add("tags", tags);
        json.addProperty("components", COMPONENT);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json.toString());
        Request request = new Request.Builder()
                .url("https://st-api.yandex-team.ru/v2/issues")
                .addHeader("Authorization", "OAuth " + stToken)
                .addHeader("User-Agent","robot-keanu-revees")
                .post(requestBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String bodyString = response.body().string();
            if (response.isSuccessful()) {
                System.out.println("Request succeed: " + bodyString);
                JsonParser parser = new JsonParser();
                JsonObject responseJson = (JsonObject) parser.parse(bodyString);
                String ticketKey = responseJson.getAsJsonPrimitive("key").getAsString();
                String link = "http://st.yandex-team.ru/" + ticketKey;
                System.out.println("Ticket: " + link);
                return link;
            } else {
                System.out.println("Request failed: " + bodyString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String tag(String value) {
        return "[" + value + "]";
    }

    private static String currentDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
}
