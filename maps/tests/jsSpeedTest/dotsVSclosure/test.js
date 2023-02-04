define(["jsSpeedTest/test-case"], function (TestCase) {
    return new TestCase({
        name: "Dots VS Closure",

        axes: ["X"],

        beforeAll: function (x) {
            var value = this;
            for (var i = 0; i < x; i++) {
                value.testVal = {
                    testData: 1
                };
                value = value.testVal;
            }

            var str = "return function () {" +
                "return testVal;" +
            "};";
            for (var i = 0; i < x; i++) {
                str = "return function () {var x = " + i + "; " + str + "}();";
            }
            var closureGenerator = new Function("testVal", str);
            //console.log("1", x, str);
            this.closureFunc = closureGenerator(value);

            var str = "return this";
            for (var i = 0; i < x; i++) {
                str += ".testVal";
            }
            str += ";";
            //console.log("2", x, str);
            this.dotFunc = new Function(str);
        },

        setUp: function () {
        },

        tearDown: function () {
        },

        afterAll: function () {
        },

        testClosure: function () {
            return this.closureFunc();
        },

        testDots: function () {
            return this.dotFunc();
        }
    });
});
