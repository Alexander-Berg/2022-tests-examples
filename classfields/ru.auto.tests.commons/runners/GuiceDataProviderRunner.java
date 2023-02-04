package ru.auto.tests.commons.runners;

import com.google.inject.Injector;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.runners.model.InitializationError;

/**
 * Created by vicdev on 11.02.17.
 */
@Deprecated
public class GuiceDataProviderRunner extends DataProviderRunner {

    private final transient Injector injector;

    public GuiceDataProviderRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        this.injector = GuiceRunnerUtils.createInjectorFor(GuiceRunnerUtils.getModulesFor(clazz));
    }

    public final Object createTest() throws Exception {
        Object obj = super.createTest();
        return GuiceRunnerUtils.injectTo(injector, obj);
    }

}
