function runSpeedTest (params) {
    if (typeof params == "function") {
        params = { action: params };
    }
    params.actionRepeats = params.actionRepeats || 100;
    params.testRepeats = params.testRepeats || 20;
    params.log = params.log || console && console.log;
    if (typeof params.onPass == "undefined" && params.log) {
        params.onPass = function (n, duration) {
            params.log('PASS ' + n + ' / ' + params.testRepeats + ', duration: ' + duration + 'ms');
        };
    }
    if (typeof params.onFinish == "undefined" && params.log) {
        params.onFinish = function (results) {
            params.log('FINISH avg: ' + results.avg + ', max: ' + results.max + ', min: ' + results.min + ', total: ' + results.total);
        };
    }

    var testsCounter = 0,
        testDuration = 0,
        actionsCounter = 0,
        makePauseEachMs = 900,
        lastPause = 0,
        results = {
            avg: 0,
            min: Infinity,
            max: 0,
            total: 0,
            tests: []
        };

    function repeatAction () {
        if (actionsCounter < params.actionRepeats) {
            var start = +new Date();
            params.action();
            testDuration += (new Date() - start);
            actionsCounter++;

            if (testDuration - lastPause > makePauseEachMs) {
                lastPause = testDuration;
                setTimeout(repeatAction, 10);
            } else {
                repeatAction();
            }
        } else {
            actionsCounter = 0;
            lastPause = 0;
            setTimeout(endTest, 10);
        }
    }
    
    function startTest () {
        testsCounter++;
        testDuration = 0;
        params.setUp && params.setUp();
        repeatAction();
    }
    
    function endTest () {
        params.tearDown && params.tearDown();
        params.onPass && params.onPass(testsCounter, testDuration);

        results.tests.push(testDuration);
        results.total += testDuration;

        if (testDuration < results.min) {
            results.min = testDuration;
        }
        if (testDuration > results.max) {
            results.max = testDuration;
        }

        if (testsCounter < params.testRepeats) {
            startTest();
        } else {
            results.avg = results.total / params.testRepeats;
            params.onFinish && params.onFinish(results);
        }
    }

    startTest();
}