package ru.yandex.autotests.mobile.disk.android.testpalm;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.gson.JsonObject;
import io.qameta.allure.Feature;
import io.qameta.allure.TmsLink;
import okhttp3.*;
import org.junit.Ignore;
import org.junit.experimental.categories.Category;
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestpalmImport {
    public static void main(String[] args) {
        try {
            String oauth = args[0];
            List<TestCaseInformation> data = collectTestCasesInformation();
            sendInformationToTestPalm(data, oauth);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<TestCaseInformation> collectTestCasesInformation() throws IOException {
        ClassLoader classLoader = TestpalmImport.class.getClassLoader();
        ImmutableSet<ClassPath.ClassInfo> classes = ClassPath.from(classLoader).getTopLevelClassesRecursive("ru.yandex.autotests.mobile.disk.android");

        List<TestCaseInformation> data = new LinkedList<>();
        for (ClassPath.ClassInfo classInfo : classes) {
            Class<?> testClass = classInfo.load();
            if (testClass.isAnnotationPresent(Feature.class)) {
                Method[] methods = testClass.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(TmsLink.class)) {
                        TmsLink tmsLink = method.getAnnotation(TmsLink.class);
                        Class<?> category = method.getAnnotation(Category.class).value()[0];
                        boolean ignored = method.isAnnotationPresent(Ignore.class);
                        data.add(new TestCaseInformation(
                                Integer.parseInt(tmsLink.value()),
                                classInfo.getName(),
                                method.getName(),
                                category,
                                ignored));
                    }
                }
            }
        }
        Collections.sort(data);
//        printLog(data);
        return data;
    }

    private static void printLog(List<TestCaseInformation> data) {
        System.out.println("Tests information");
        for (TestCaseInformation testCaseInformation : data) {
            System.out.println(testCaseInformation.toString());
        }
    }

    private static void sendInformationToTestPalm(List<TestCaseInformation> data, String oauth) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        for (TestCaseInformation testCaseInformation : data) {
            String json = testCaseInformation.serialize();
            Request request = new Request.Builder()
                    .url("https://testpalm-api.yandex-team.ru/testcases/adisk")
                    .addHeader("Authorization", "OAuth " + oauth)
                    .method("PATCH", RequestBody.create(MediaType.parse("application/json"), json))
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                System.out.println("Updating test case #" + testCaseInformation.link + ", result:" + response.code());
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Updating test case #" + testCaseInformation.link + ", result: failed");
            }
        }
    }

    public static class TestCaseInformation implements Comparable<TestCaseInformation> {
        final int link;
        final String className;
        final String methodName;
        final Class category;
        final boolean ignored;

        public TestCaseInformation(int link, String className, String methodName, Class category, boolean ignored) {
            this.link = link;
            this.className = className;
            this.methodName = methodName;
            this.category = category;
            this.ignored = ignored;
        }

        public String serialize() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", link);
            jsonObject.addProperty("isAutotest", category != Quarantine.class && !ignored);
            return jsonObject.toString();
        }

        @Override
        public String toString() {
            return "TestCaseInformation{" +
                    "link=" + link +
                    ", className='" + className + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", ignored=" + (category == Quarantine.class || ignored) +
                    '}';
        }

        @Override
        public int compareTo(TestCaseInformation o) {
            return Integer.compare(link, o.link);
        }
    }
}
