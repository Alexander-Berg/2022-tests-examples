requirejs.config({
    paths: {
        "jsSpeedTest": ".."
    }
});

require([
    "jsSpeedTest/test-runner", "jsSpeedTest/test-runner-view", "test"
], function (TestRunner, TestRunnerView, test) {
    var testRunner = new TestRunner();

    new TestRunnerView(testRunner, "#log");

    testRunner.add(test, {
        allowableError: 0.01
    });

    testRunner.run();
});
