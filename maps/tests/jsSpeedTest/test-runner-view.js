define(["jquery", "./flot/jquery.flot", "./flot/jquery.flot.categories", "./flot/excanvas.min"], function ($) {
    function TestRunnerView (testRunner, domElement) {
        this._testRunner = testRunner;
        this._domElement = $(domElement);

        var _this = this;
        $(this._testRunner).on("complete", function (e) {
            _this._onComplete(e);
        });
    }

    var ptp = TestRunnerView.prototype;

    ptp._onComplete = function (event) {
        var state = event.target.runningState;
        for (var i = 0, l = state.results.length; i < l; i++) {
            this._processTestCase(state.testCases[i], state.results[i]);
        }
    };

    ptp._processTestCase = function (testCase, results) {
        var axesNumber = testCase.axes.length;
        switch (axesNumber) {
            case 0:
                this._drawBars(testCase, results, this._getChartContainer());
                break;
            case 1:
                this._drawLines(testCase, results, this._getChartContainer());
                break;
            default:
                throw new Error("Отображение графиков с размерностью " + axesNumber + " не поддерживается.");
        }

    };

    ptp._drawBars = function (testCase, results, domElement) {
        var info = [];
        for (var i = 0, l = testCase.tests.length; i < l; i++) {
            info.push([testCase.tests[i], results[i]]);
        }
        $.plot(domElement, [ info ], {
            series: {
                bars: {
                    show: true,
                    barWidth: 0.9,
                    align: "center"
                }
            },
            xaxis: {
                mode: "categories",
                tickLength: 0
            }
        });
    };

    ptp._drawLines = function (testCase, results, domElement) {
        var info = [];
        for (var i = 0, l = testCase.tests.length; i < l; i++) {
            var data = [];
            for (var n in results) {
                if (results.hasOwnProperty(n)) {
                    data.push([n, results[n][i]]);
                }
            }
            info.push({
                data: data,
                label: testCase.tests[i]
            });
        }
        var plot = $.plot(domElement, info, {
            series: {
                lines: {
                    show: true
                },
                points: {
                    show: true
                }
            },
            grid: {
                hoverable: true,
                clickable: true
            }
        });
    };

    ptp._getChartContainer = function () {
        var result = $("<div></div>").css({
            width: "50%",
            height: "500px"
        });
        this._domElement.append(result);
        return result;
    };

    return TestRunnerView;
});
