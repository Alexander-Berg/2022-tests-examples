define(["jquery"], function ($) {
    function TestRunner (params) {
        this._params = params;
        this._testCases = [];
        this._testCasesParams = [];

        this.runningState = null;
    }

    var ptp = TestRunner.prototype,
        defaultTestCaseParams = {
            axesRanges: [[1, 10], [1, 10]],
            axesSteps: [4, 4],
            allowableError: 0.1,
            integerAxes: false
        };

    ptp.add = function (testCase, params) {
        params = $.extend({}, defaultTestCaseParams, params);

        var testCaseAxes = testCase.axes.length;
        if (testCaseAxes > params.axesRanges.length) {
            throw new Error("Не переданы диапазоны значений параметров для теста \"" + testCase.name + "\"");
        }

        this._testCases.push(testCase);
        this._testCasesParams.push(params);
    };

    ptp.remove = function (testCase) {
        var index = $.inArray(testCase, this._testCases);
        if (index != -1) {
            this._testCases.splice(index, 1);
            this._testCasesParams.splice(index, 1);
        }
    };

    ptp.run = function () {
        if (!this.runningState) {
            this.runningState = {
                testCases: this._testCases.slice(),
                testCasesParams: this._testCasesParams.slice(),
                testCaseIndex: -1,
                testIndex: -1,
                results: []
            };
            this._runNextTestCase();
        }
    };

    ptp._runNextTestCase = function () {
        var state = this.runningState,
            newTestCaseIndex = state.testCaseIndex + 1,
            testCase = state.testCases[newTestCaseIndex];
        if (testCase) {
            console.log(testCase.name);
            state.testCaseIndex = newTestCaseIndex;
            state.results[newTestCaseIndex] = this._runTestCaseOnAxes(
                testCase, state.testCasesParams[newTestCaseIndex], []
            );

            var _this = this;
            setTimeout(function () {
                _this._runNextTestCase();
            }, 200);
        } else {
            console.log(state.results);
            $(this).trigger("complete");
        }
    };

    ptp._runTestCaseOnAxes = function (testCase, testCaseParams, axesValues) {
        if (testCase.axes.length != axesValues.length) {
            var result = {},
                axisRange = testCaseParams.axesRanges[axesValues.length],
                axisMin = axisRange[0],
                axisMax = axisRange[1],
                steps = testCaseParams.axesSteps,
                stepIncrement = (axisMax - axisMin) / (steps - 1);
            for (var i = 0; i < steps; i++) {
                var axisValue = i < steps - 1 ? axisMin + (i * stepIncrement) : axisMax;
                if (testCaseParams.integerAxes) {
                    axisValue = Math.round(axisValue);
                }
                axesValues.push(axisValue);
                result[axisValue] = this._runTestCaseOnAxes(
                    testCase, testCaseParams, axesValues
                );
                axesValues.pop();
            }
            return result;
        } else {
            return this._runTestCase(testCase, testCaseParams, axesValues);
        }
    };

    ptp._runTestCase = function (testCase, testCaseParams, axesValues) {
        if (testCase.beforeAll) {
            testCase.beforeAll(axesValues[0], axesValues[1]);
        }
        this.runningState.testIndex = -1;
        var testCaseResults = [];
        this._runNextTest(testCase, testCaseParams, testCaseResults, axesValues);
        return testCaseResults;
    };

    ptp._runNextTest = function (testCase, testCaseParams, testCaseResults, axesValues) {
        var state = this.runningState,
            newTestIndex = state.testIndex + 1,
            testName = testCase.tests[newTestIndex];
        if (testName) {
            state.testIndex = newTestIndex;
            this._runTest(testName, testCase, testCaseParams, testCaseResults, axesValues);
        } else {
            if (testCase.afterAll) {
                testCase.afterAll(axesValues[0], axesValues[1]);
            }
        }
    };

    ptp._runTest = function (testName, testCase, testCaseParams, testCaseResults, axesValues) {
        testCaseResults[this.runningState.testIndex] = this._calcTest(
            testName, testCase, testCaseParams, axesValues
        );

        this._runNextTest(testCase, testCaseParams, testCaseResults, axesValues);
    };

    ptp._calcTest = function (testName, testCase, testCaseParams, axesValues) {
        var runNumber = this._getCalibratedUnitsNumber(testCase, testName, testCaseParams.allowableError, axesValues),
            results = [],
            avg,
            newAvg;

        for (var i = 0; i < 1/testCaseParams.allowableError; i++) {
            results[i] = this._runTestOnUnits(testCase, testName, runNumber, axesValues)/runNumber;
        }
        newAvg = getAvg(results);

        do {
            avg = newAvg;
            results.push(this._runTestOnUnits(testCase, testName, runNumber, axesValues)/runNumber);
            newAvg = getAvg(results);
        } while (Math.abs(avg - newAvg)/newAvg > testCaseParams.allowableError);
        return newAvg;
    };

    ptp._getCalibratedUnitsNumber = function (testCase, testName, allowableError, axesValues) {
        var targetTime = 1/allowableError,
            unitsCnt = 1,
            time;
        while ((time = this._runTestOnUnits(testCase, testName, unitsCnt, axesValues)) < targetTime) {
            unitsCnt = Math.round(unitsCnt * targetTime/(time || 1));
        }
        console.log(testName + " calibration", time, unitsCnt, allowableError, axesValues.toString());

        return unitsCnt;
    };

    ptp._runTestOnUnits = function (testCase, testName, unitsCnt, axesValues) {
        var unitClass = testCase.___unitClass,
            units = unitClass.units,
            dateNow = Date.now,
            xValue = axesValues[0],
            yValue = axesValues[1],
            i;

        for (i = units.length; i < unitsCnt; i++) {
            units[i] = new unitClass();
        }

        if (testCase.setUp) {
            for (i = 0; i < unitsCnt; i++) {
                units[i].setUp(xValue, yValue);
            }
        }

        var beginTime = dateNow ? Date.now() : new Date();
        for (i = 0; i < unitsCnt; i++) {
            units[i][testName](xValue, yValue);
        }
        var endTime = dateNow ? Date.now() : new Date();

        if (testCase.tearDown) {
            for (i = 0; i < unitsCnt; i++) {
                units[i].tearDown(xValue, yValue);
            }
        }

        return endTime - beginTime;
    };

    ptp.stop = function (reset) {
        //TODO
    };

    function getAvg (arr) {
        for (var i = 0, l = arr.length, sum = 0; i < l; i++) {
            sum += arr[i];
        }
        return sum/l;
    }

    return TestRunner;
});
