package ru.auto.tests.commons.runners;

import com.google.inject.Injector;
import lombok.extern.log4j.Log4j;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

/**
 * eroshenkoam
 * 26.01.17
 */
@Log4j
public class GuiceParametersRunnerFactory implements ParametersRunnerFactory {

    @Override
    public Runner createRunnerForTestWithParameters(TestWithParameters testWithParameters) throws InitializationError {
        Class testClass = testWithParameters.getTestClass().getJavaClass();
        log.info(String.format("Read modules for: %s", testClass));

        Class<?>[] modules = GuiceRunnerUtils.getModulesFor(testClass);
        final Injector injector = GuiceRunnerUtils.createInjectorFor(modules);
        return new BlockJUnit4ClassRunnerWithParameters(testWithParameters) {
            public final Object createTest() throws Exception {
                Object testObject = super.createTest();
                return GuiceRunnerUtils.injectTo(injector, testObject);
            }
        };
    }
}
