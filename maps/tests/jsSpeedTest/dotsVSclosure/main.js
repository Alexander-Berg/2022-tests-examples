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
        allowableError: 0.02,
        axesRanges: [[1, 8]],
        axesSteps: [8],
        integerAxes: true
    });

    testRunner.run();
});
