package ru.yandex.realty.allure;

import io.qameta.allure.junit4.AllureJunit4;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.runner.notification.RunNotifier;

@Aspect
public class AllureJunit4ListenerAspect2 {

    private final AllureJunit4 allure = new AllureJunit4();

    @Pointcut("execution(org.junit.runner.notification.RunNotifier.new())")
    public void withoutSurefireExecution() {
    }

    @After("withoutSurefireExecution()")
    public void addListener(final JoinPoint point) {
        if (point.getThis().getClass().equals(RunNotifier.class)) {
            final RunNotifier notifier = (RunNotifier) point.getThis();
            notifier.removeListener(allure);
            notifier.addListener(allure);
        }
    }

}
