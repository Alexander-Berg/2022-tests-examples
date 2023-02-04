define(["jsSpeedTest/test-case"], function (TestCase) {
    return new TestCase({
        name: "Mixin vs delegation",

        axes: ["X"],

        beforeAll: function (x) {
            // Миксин на уровне класса.
            var mixinFuncs = "",
                mixinAssump = "",
                mixinBody;
            for (var i = 0; i < x; i++) {
                mixinFuncs += "var func" + i + " = function () {};";
                mixinAssump += "ptp.func" + i + " = func" + i + ";";
            }
            mixinBody = mixinFuncs + "return function (ptp) {" + mixinAssump + "}";
            this.mixIn = (new Function("ptp", mixinBody))();
            this.MyClassMixin = function () {
                this.func0();
            };
            this.mixIn(this.MyClassMixin.prototype);

            this.objMix = new this.MyClassMixin();

            // Класс, делегирующий часть ответсвенности компоненту.
            var Component = function () {},
                ptp = Component.prototype;
            for (var i = 0; i < x; i++) {
                ptp["func" + i] = function () {};
            }
            this.MyClassDeleg = function () {
                this._component = new Component();
            };
            ptp = this.MyClassDeleg.prototype;
            for (var i = 0; i < x; i++) {
                ptp["func" + i] = new Function("this._component.func" + i + "();");
            }

            this.objDel = new this.MyClassDeleg();

            // Миксин на уровне объектов.
            var ObjectMixin = this.ObjectMixin = function () {};
                ptp = ObjectMixin.prototype;
            for (var i = 0; i < x; i++) {
                ptp["func" + i] = function () {};
            }

            ObjectMixin.mixin = function (obj) {
                var mixedObject = new ObjectMixin();

                for (var i = 0; i < x; i++) {
                    obj["func" + i] = mixedObject["func" + i].bind(obj);
                }
            };
            this.MyClassObjectMixin = function () {};

            this.objObjMix = new this.MyClassObjectMixin();
            this.ObjectMixin.mixin(this.objObjMix);
        },

        setUp: function () {

        },

        tearDown: function () {
        },

        afterAll: function () {

        },

        testMixin: function (x) {
            for (var i = 0; i < x; i++) {
                this.objMix["func" + i]();
            }
        },

        testDelegation: function (x) {
            for (var i = 0; i < x; i++) {
                this.objDel["func" + i]();
            }
        },

        testObjectMixin: function (x) {
            for (var i = 0; i < x; i++) {
                this.objObjMix["func" + i]();
            }
        }
    });
});
