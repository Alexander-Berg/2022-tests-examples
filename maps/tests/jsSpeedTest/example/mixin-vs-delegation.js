define(["jsSpeedTest/test-case"], function (TestCase) {
    return new TestCase({
        name: "Mixin vs delegation",

        axes: ["X"],

        beforeAll: function (x) {
            var mixinFuncs = "",
                mixinAssump = "",
                mixinBody;
            for (var i = 0; i < x; i++) {
                mixinFuncs += "var func" + i + " = function () {};";
                mixinAssump += "ptp.func" + i + " = func" + i + ";";
            }
            mixinBody = mixinFuncs + "return function (ptp) {" + mixinAssump + "}";
            this.mixIn = (new Function("ptp", mixinBody))();
        },

        setUp: function () {
            this.MyClass = function () {
            };
        },

        tearDown: function () {
            this.MyClass = null;
        },

        afterAll: function () {

        },

        testMixin: function () {
            this.mixIn(this.MyClass.prototype);
        },

        testDelegation: function (x) {
            for (var i = 0; i < x; i++) {
                this.MyClass.prototype["func" + i] = function () {
                    // прокидывание вызова
                }
            }
        }
    });
});
