package ru.yandex.navi.tf;

import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

@SuppressWarnings("WeakerAccess")
public class RetryRunner extends BlockJUnit4ClassRunner {
    private enum TestResult {
        SUCCESS,
        ERROR,
        FATAL
    }

    public RetryRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        if (isIgnored(method)) {
            notifier.fireTestIgnored(description);
            return;
        }

        final int count = getRetryCount();
        for (int i = 0; i < count; i++) {
            TestResult result = runTest(methodBlock(method), description, notifier, i, count);
            if (result == TestResult.SUCCESS)
                return;
            if (result == TestResult.FATAL)
                break;
        }
        System.err.println("ru.yandex.navi.tf.RetryRunner: Test failed after multiple retries");
    }

    private static int getRetryCount() {
        String envRetryCount = System.getenv("RETRY_COUNT");
        if (envRetryCount != null)
            return Integer.parseInt(envRetryCount);

        return 3;
    }

    private TestResult runTest(Statement statement, Description description, RunNotifier notifier,
                               int i, int count) {
        EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
        eachNotifier.fireTestStarted();
        try {
            statement.evaluate();
            return TestResult.SUCCESS;
        } catch (Throwable e) {
            if (e instanceof AssumptionViolatedException)
                eachNotifier.addFailedAssumption((AssumptionViolatedException) e);
            else
                eachNotifier.addFailure(e);

            if (isFatalError(e))
                return TestResult.FATAL;

            if (count > 0 && i == count - 1) {
                eachNotifier.addFailure(new NoRetryException(
                        "Test failed after multiple retries. Last error: " + e.getMessage()));
                if (System.getenv("SANDBOX_TOKEN") != null) {
                    System.err.println("##teamcity[buildStatus status='FAILURE'"
                        + " text='Test failed after multiple retries.']");
                }
            }

            return TestResult.ERROR;
        } finally {
            eachNotifier.fireTestFinished();
        }
    }

    static boolean isFatalError(Throwable error) {
        if (error instanceof MultipleFailureException) {
            for (Throwable it : ((MultipleFailureException) error).getFailures()) {
                if (isFatalError(it))
                    return true;
            }
            return false;
        }

        return error instanceof NoRetryException;
    }
}
