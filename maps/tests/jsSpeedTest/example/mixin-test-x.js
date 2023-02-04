define(["jsSpeedTest/test-case"], function (TestCase) {
    return new TestCase({
        name: "Prototype mixin techniques",

        axes: ["X"],

        beforeAll: function (x) {
            this.circleFns = {};
            for (var i = 0; i < x; i++) {
                this.circleFns["func" + i] = function () {};
            }

            var asCircleBody = "";
            for (var i = 0; i < x; i++) {
                asCircleBody += "ptp.func" + i + " = function () {};"
            }
            this.asCircle = new Function("ptp", asCircleBody);

            var asCircleCachedFuncs = "",
                asCircleCachedAssum = "",
                asCircleCachedBody;
            for (var i = 0; i < x; i++) {
                asCircleCachedFuncs += "var func" + i + " = function () {};";
                asCircleCachedAssum += "ptp.func" + i + " = func" + i + ";";
            }
            asCircleCachedBody = asCircleCachedFuncs + "return function (ptp) {" + asCircleCachedAssum + "}";
            this.asCircleCached = (new Function("ptp", asCircleCachedBody))();

            this.CircularObject = function (radius) {
                this.radius = radius
            };
        },

        setUp: function () {

        },

        tearDown: function () {

        },

        afterAll: function () {
            this.CircularObject = null;
        },

        testFuncWithoutCache: function (x) {
            this.asCircle(this.CircularObject.prototype);
        },

        testFuncWithCache: function (x) {
            this.asCircleCached(this.CircularObject.prototype);
        },

        testExtend: function (x) {
            var circleFns = this.circleFns,
                ptp = this.CircularObject.prototype;
            for (var k in circleFns) {
                if (circleFns.hasOwnProperty(k)) {
                    ptp[k] = circleFns[k];
                }
            }
        },

        testExtendByKeys: function (x) {
            var circleFns = this.circleFns,
                ptp = this.CircularObject.prototype,
                keys = Object.keys(circleFns);
            for (var i = 0, l = keys.length; i < l; i++) {
                var key = keys[i];
                ptp[key] = circleFns[key];
            }
        }
    });
});
