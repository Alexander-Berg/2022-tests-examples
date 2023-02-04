package ru.yandex.payments.testing.micronaut_cucumber;

import io.cucumber.java.Scenario;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.context.scope.refresh.RefreshScope;
import io.micronaut.test.condition.TestActiveCondition;
import io.micronaut.test.extensions.junit5.MicronautJunit5Extension;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.AfterClass;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("HideUtilityClassConstructor")
public class MicronautCucumberTest {
    public static final String EXTRA_GLUE = "ru.yandex.payments.testing.micronaut_cucumber";

    private static final Namespace MICRONAUT_NAMESPACE = Namespace.create(MicronautJunit5Extension.class);
    private static MicronautJunit5Extension extension = null;
    private static MicronautCucumberExtensionContext context = null;

    static ApplicationContext getMicronautContext() {
        val store = context.getStore(MICRONAUT_NAMESPACE);
        return requireNonNull(store.get(ApplicationContext.class, ApplicationContext.class));
    }

    @SneakyThrows
    protected static void init(Class<?> testClass) {
        context = new MicronautCucumberExtensionContext(testClass);
        extension = new MicronautJunit5Extension();
        extension.beforeAll(context);
    }

    @AfterClass
    @SneakyThrows
    public static void release() {
        if (extension != null) {
            extension.afterAll(context);
            extension = null;
        }

        if (context != null) {
            context.close();
            context = null;
        }
    }

    @SneakyThrows
    static void beforeScenario(Scenario scenario) {
        context.setUniqueId(scenario.getId());
        context.setScenarioName(scenario.getName());
        extension.beforeEach(context);
        getMicronautContext().findBean(RefreshScope.class).ifPresent(refreshScope -> {
            refreshScope.onRefreshEvent(new RefreshEvent(singletonMap(
                    TestActiveCondition.ACTIVE_MOCKS, "changed"
            )));
        });
    }

    @SneakyThrows
    static void afterScenario(Scenario scenario) {
        extension.afterEach(context);
        context.setUniqueId("");
        context.setScenarioName("");
    }
}
