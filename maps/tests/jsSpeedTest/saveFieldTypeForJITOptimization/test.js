var str = "test";

define(["jsSpeedTest/test-case"], function (TestCase) {
    return new TestCase({
        name: "Save field type test",

        axes: [],

        beforeAll: function () {
            this.objStr = {
                test: ""
            };
            this.objObj = {
                test: null
            };
        },

        setUp: function () {
            this.objStr.test = "";
            this.objObj.test = null;
        },

        tearDown: function () {
        },

        afterAll: function () {
        },

        testNonSave: function () {
            this.objObj = str;
        },

        testSave: function () {
            this.objStr = str;
        }
    });
});
