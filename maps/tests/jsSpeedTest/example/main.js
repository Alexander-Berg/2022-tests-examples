requirejs.config({
    paths: {
        "jsSpeedTest": ".."
    }
});

require([
    "jsSpeedTest/test-runner", "jsSpeedTest/test-runner-view", "mixin-test-x", "mixin-test", "mixin-vs-delegation-constructors"
], function (TestRunner, TestRunnerView, mixinTestX, mixinTest, mixinVsDelegation) {
    var testRunner = new TestRunner();

    new TestRunnerView(testRunner, "#log");

    testRunner.add(mixinTestX, {
        allowableError: 0.05,
        axesRanges: [[1, 100]],
        axesSteps: [10],
        integerAxes: true
    });

//    testRunner.add(mixinTest, {
////        allowableError: 0.03
//    });

    testRunner.add(mixinVsDelegation, {
        allowableError: 0.05,
        axesRanges: [[1, 50]],
        axesSteps: [5],
        integerAxes: true
    });

    testRunner.run();
});
