package ru.yandex.autotests.mobile.disk.android.infrastructure.suites;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import io.qameta.allure.Feature;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Acceptance;
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic;
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress;
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression;
import ru.yandex.autotests.mobile.disk.android.props.AndroidConfig;
import ru.yandex.autotests.mobile.disk.android.rules.AppConfigRuleKt;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

@RunWith(FeatureAllTests.class)
public class FeatureSuite {
    private static final Map<String, List<Class>> featuresMap = new HashMap<>();

    static {
        try {
            createFeaturesMap();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createFeaturesMap() throws IOException {
        ClassLoader classLoader = FeatureSuite.class.getClassLoader();
        ImmutableSet<ClassPath.ClassInfo> classes = ClassPath.from(classLoader)
                .getTopLevelClassesRecursive("ru.yandex.autotests.mobile.disk.android");
        for (ClassPath.ClassInfo classInfo : classes) {
            Class<?> testClass = classInfo.load();
            if (testClass.isAnnotationPresent(Feature.class)) {
                Feature feature = testClass.getAnnotation(Feature.class);
                if (featuresMap.containsKey(feature.value())) {
                    featuresMap.get(feature.value()).add(testClass);
                } else {
                    List<Class> list = new ArrayList<>();
                    list.add(testClass);
                    featuresMap.put(feature.value(), list);
                }
            }
        }
    }

    public static TestSuite suite() throws NoTestsRemainException {
        AndroidConfig props = AndroidConfig.props();
        List<Class> categories = getCategories(props.categories());
        Filter filter = createCategoryFilter(categories);
        List<Class> testClasses = getTestClasses(props.features(), categories);
        return createTestSuite(filter, testClasses);
    }

    private static List<Class> getCategories(String categories) {
        List<Class> categoriesClasses = Arrays.asList(Acceptance.class, BusinessLogic.class, Regression.class, FullRegress.class);
        if (categories == null || categories.isEmpty()) return categoriesClasses;

        List<Class> selected = new ArrayList<>();
        for (String category : categories.split(",")) {
            for (Class<?> aClass : categoriesClasses) {
                if (category.equalsIgnoreCase(aClass.getSimpleName())) {
                    selected.add(aClass);
                    break;
                }
            }
        }

        return selected;
    }

    private static TestSuite createTestSuite(Filter filter, List<Class> testClasses) throws NoTestsRemainException {
        TestSuite suite = new TestSuite();
        for (Class testClass : testClasses) {
            JUnit4TestAdapter adapter = new JUnit4TestAdapter(testClass);
            if (filter != null) {
                adapter.filter(filter);
            }
            suite.addTest(adapter);
        }
        return suite;
    }

    private static List<Class> getTestClasses(String features, List<Class> category) {
        List<Class> testClasses = new LinkedList<>();
        if (features == null || features.isEmpty()) {
            for (List<Class> list : featuresMap.values()) {
                testClasses.addAll(list);
            }
        } else {
            for (String feature : features.split(",")) {
                testClasses.addAll(featuresMap.get(feature));
            }
        }
        return getCategoryTestsClasses(testClasses, category);
    }

    private static List<Class> getCategoryTestsClasses(List<Class> testClasses, List<Class> categories) {
        if (categories == null || categories.isEmpty()) return testClasses;

        List<Class> res = new LinkedList<>();
        for (Class testClass : testClasses) {
            boolean testClassHasCategoryTests = false;
            for (Method method : testClass.getMethods()) {
                if (method.isAnnotationPresent(Test.class) && !method.isAnnotationPresent(Ignore.class)) {
                    Class<?>[] methodCategories = method.getAnnotation(Category.class).value();
                    for (Class<?> categoryClass : methodCategories) {
                        if (categories.contains(categoryClass)) {
                            res.add(testClass);
                            testClassHasCategoryTests = true;
                            break;
                        }
                    }
                }
                if (testClassHasCategoryTests) {
                    break;
                }
            }
        }
        return res;
    }

    @Nullable
    private static Filter createCategoryFilter(List<Class> categories) {
        if (categories == null || categories.isEmpty()) return null;
        return new CategoryFilter(categories);
    }

    private static class CategoryFilter extends Filter {

        private final List<Class> categories;

        public CategoryFilter(List<Class> categories) {
            this.categories = categories;
        }

        @Override
        public boolean shouldRun(Description description) {
            List<Category> allAnnotations = AppConfigRuleKt.getAllAnnotations(description, Category.class);
            for (Category annotation : allAnnotations) {
                for (Class<?> aClass : annotation.value()) {
                    if (categories.contains(aClass)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public String describe() {
            return null;
        }
    }
}
