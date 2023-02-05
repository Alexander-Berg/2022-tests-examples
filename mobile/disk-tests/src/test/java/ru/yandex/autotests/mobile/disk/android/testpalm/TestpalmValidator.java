package ru.yandex.autotests.mobile.disk.android.testpalm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TestpalmValidator {
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public static void main(String[] args) {
        try {
            String oauth = args[0];
            List<TestpalmImport.TestCaseInformation> tests = TestpalmImport.collectTestCasesInformation();
            Map<Class, List<Integer>> cases = collectCases(oauth);
            for (TestpalmImport.TestCaseInformation information : tests) {
                if (cases.get(information.category).contains(information.link)) {
                    System.out.println("Test #" + information.link + " is ok");
                } else {
                    String category = findCategory(cases, information.link);
                    if (category != null) {
                        System.out.println("Test #" + information.link + " should be " + category);
                    } else {
                        String categoryFromTestpalm = getCategoryFromTestpalm(information.link, oauth);
                        System.out.println("Test #" + information.link + " is in quarantine. Should be " + categoryFromTestpalm);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getCategoryFromTestpalm(int link, String oauth) throws IOException {
        String expression = String.format("{\"type\": \"EQ\",\"key\": \"id\",\"value\": \"%d\"}", link);
        Request request = new Request.Builder()
                .url("https://testpalm-api.yandex-team.ru/testcases/adisk?include=id,attributes&expression=" + expression)
                .addHeader("Authorization", "OAuth " + oauth)
                .get()
                .build();
        try(Response response = httpClient.newCall(request).execute()) {
            JsonArray array = (JsonArray) new JsonParser().parse(response.body().charStream());
            if (array.size() == 0) return null;
            JsonObject caseObject = (JsonObject) array.get(0);
            return getCategory(caseObject, link).getSimpleName();
        }
    }

    private static Map<Class, List<Integer>> collectCases(String oauth) throws IOException {
        String expression = "{\"type\": \"EQ\",\"key\": \"isAutotest\",\"value\": \"true\"}";
        Request request = new Request.Builder()
                .url("https://testpalm-api.yandex-team.ru/testcases/adisk?include=id,attributes&expression=" + expression)
                .addHeader("Authorization", "OAuth " + oauth)
                .get()
                .build();
        Map<Class, List<Integer>> cases = new HashMap<>();
        cases.put(Acceptance.class, new LinkedList<>());
        cases.put(BusinessLogic.class, new LinkedList<>());
        cases.put(Regression.class, new LinkedList<>());
        cases.put(FullRegress.class, new LinkedList<>());
        cases.put(Quarantine.class, Collections.emptyList());
        try (Response response = httpClient.newCall(request).execute()) {
            JsonArray casesJson = (JsonArray) new JsonParser().parse(response.body().charStream());
            for (JsonElement jsonElement : casesJson) {
                JsonObject caseObject = (JsonObject) jsonElement;
                int id = caseObject.get("id").getAsInt();
                Class category = getCategory(caseObject, id);
                cases.get(category).add(id);
            }
        }
        return cases;
    }

    private static Class getCategory(JsonObject caseObject, int id) {
        JsonObject attributes = caseObject.getAsJsonObject("attributes");
        String feature = "";
        if (attributes.has("5e00f1e646af84170c901e54")) {
            feature = attributes.get("5e00f1e646af84170c901e54").getAsJsonArray().get(0).getAsString();
        } else {
            System.out.println("Case " + id + " doesn't have feature priority");
        }
        String testcase = "";
        if (attributes.has("5df111709fcbfa31fc1308fb")) {
            testcase = attributes.get("5df111709fcbfa31fc1308fb").getAsJsonArray().get(0).getAsString();
        } else {
            System.out.println("Case " + id + " doesn't have testcase priority");
        }
        return getCategory(feature, testcase);
    }

    private static Class getCategory(String feature, String testcase) {
        if ("High".equalsIgnoreCase(feature) && "High".equalsIgnoreCase(testcase)) {
            return Acceptance.class;
        } else if ("High".equalsIgnoreCase(feature) && "Medium".equalsIgnoreCase(testcase)
                || "Medium".equalsIgnoreCase(feature) && "High".equalsIgnoreCase(testcase)) {
            return BusinessLogic.class;
        } else if ("High".equalsIgnoreCase(feature) && "Low".equalsIgnoreCase(testcase)
                || "Low".equalsIgnoreCase(feature) && "High".equalsIgnoreCase(testcase)
                || "Medium".equalsIgnoreCase(feature) && "Medium".equalsIgnoreCase(testcase)) {
            return Regression.class;
        }
        return FullRegress.class;
    }

    private static String findCategory(Map<Class, List<Integer>> cases, int link) {
        for (Map.Entry<Class, List<Integer>> entry : cases.entrySet()) {
            if (entry.getValue().contains(link)) {
                return entry.getKey().getSimpleName();
            }
        }
        return null;
    }
}
