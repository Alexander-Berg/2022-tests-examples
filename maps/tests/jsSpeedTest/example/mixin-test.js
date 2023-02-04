define(["jsSpeedTest/test-case"], function (TestCase) {
    return new TestCase({
        name: "Prototype mixin techniques",

        axes: [],

        beforeAll: function () {
            var x = 30;

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
                asCircleCachedAssump = "",
                asCircleCachedBody;
            for (var i = 0; i < x; i++) {
                asCircleCachedFuncs += "var func" + i + " = function () {};";
                asCircleCachedAssump += "ptp.func" + i + " = func" + i + ";";
            }
            asCircleCachedBody = asCircleCachedFuncs + "return function (ptp) {" + asCircleCachedAssump + "}";
            this.asCircleCached = (new Function("ptp", asCircleCachedBody))();
        },

        setUp: function () {
            this.CircularObject = function (radius) {
                this.radius = radius
            };
        },

        tearDown: function () {
            this.CircularObject = null;
        },

        afterAll: function () {

        },

        testFuncWithoutCache: function () {
            this.asCircle(this.CircularObject.prototype);
        },

        testFuncWithCache: function () {
            this.asCircleCached(this.CircularObject.prototype);
        },

        testExtend: function () {
            var circleFns = this.circleFns,
                ptp = this.CircularObject.prototype;
            for (var k in circleFns) {
                if (circleFns.hasOwnProperty(k)) {
                    ptp[k] = circleFns[k];
                }
            }
        },

        testExtendByKeys: function () {
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
