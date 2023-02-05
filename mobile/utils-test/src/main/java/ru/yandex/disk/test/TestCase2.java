package ru.yandex.disk.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@RunWith(RobolectricTestRunner.class)
public abstract class TestCase2 extends Assert2 {

    private static Class<? extends TestCase2> lastCheckedClass;

    @Before
    public void before() throws Exception {
        checkTestMethods();
        setUp();
        ShadowLog.stream = System.out;
    }

    private void checkTestMethods() {
        Class<? extends TestCase2> klass = getClass();

        if (klass == lastCheckedClass) {
            return;
        }

        lastCheckedClass = klass;

        while (!klass.equals(TestCase2.class)) {
            checkTestMethods(klass);
            klass = (Class<? extends TestCase2>) klass.getSuperclass();
        }
    }

    private void checkTestMethods(Class<? extends TestCase2> klass) {
        for (Method method : klass.getDeclaredMethods()) {
            int m = method.getModifiers();
            if (Modifier.isPublic(m) &&
                    !Modifier.isAbstract(m) &&
                    !Modifier.isStatic(m) &&
                    method.getName().startsWith("test") &&
                    method.getParameterTypes().length == 0) {
                checkContainsTestAnnotation(method);
            }
        }
    }

    private void checkContainsTestAnnotation(Method method) {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation a : annotations) {
            if (a.annotationType().equals(Test.class)) {
                return;
            }
        }
        throw new AssertionError("Missed Test annotation for " + method);
    }

    protected void setUp() throws Exception {
    }

    @After
    public void after() throws Exception {
        tearDown();
    }

    protected void tearDown() throws Exception {
        Reflector.scrub(this);
    }
}
