var slice = Array.prototype.slice;

define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Dots vs Closure',

        axes: [],

        beforeAll: function () {
            this.array = [1, 2, 3];
        },

        setUp: function () {
        },

        tearDown: function () {
        },

        afterAll: function () {
        },

        testClosure: function () {
            return slice.call(this.array, 1);
        },

        testDots: function () {
            return Array.prototype.slice.call(this.array, 1);
        }
    });
});
