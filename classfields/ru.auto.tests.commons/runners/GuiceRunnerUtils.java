package ru.auto.tests.commons.runners;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import lombok.extern.log4j.Log4j;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vicdev on 12.02.17.
 * Warning: duplicate code from GuiceTestRunner...
 */
@Log4j
public class GuiceRunnerUtils {

    static Object injectTo(Injector injector, Object obj) {
        injector.injectMembers(obj);
        log.info(String.format("Inject dependencies for test: %s", obj));
        return obj;
    }

    static Injector createInjectorFor(Class<?>... classes) throws InitializationError {
        List<Module> modules = new ArrayList<>();
        for (Object module : Arrays.asList(classes)) {
            Class moduleClass = (Class) module;
            try {
                modules.add((Module) moduleClass.newInstance());
            } catch (ReflectiveOperationException var6) {
                throw new InitializationError(var6);
            }
        }
        Injector injector = Guice.createInjector(modules);
        log.info(String.format("Create injector: %s", injector));
        return injector;
    }

    static Class<?>[] getModulesFor(Class<?> testClass) throws InitializationError {
        GuiceModules annotation = testClass.getAnnotation(GuiceModules.class);
        if (annotation == null) {
            String message = String.format("Missing @GuiceModules annotation for unit test \'%s\'", testClass.getName());
            throw new InitializationError(message);
        } else {
            log.info(String.format("Detected modules: %s", Arrays.toString(annotation.value())));
            return annotation.value();
        }
    }
}
