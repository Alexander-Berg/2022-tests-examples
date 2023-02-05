define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Fire With Priority',

        axes: ['Listeners'],

        beforeAll: function (x) {
            this.callbacks = [];
            for (var i = 0; i < x; i++) {
                this.callbacks[i] = ['click', function () {}, i];
            }

            this.randCallbacks = [];
            var callbacksCopy = this.callbacks.slice();
            for (var i = 0; i < x; i++) {
                this.randCallbacks.push(callbacksCopy.splice(Math.floor(Math.random() * callbacksCopy.length), 1)[0]);
            }

            this.event = new ymaps.Event();
        },

        setUp: function (x) {
            this.newManager = new ymaps.event.manager.Base();
            this.priorityManager = new oldYMaps.PriorityManager();

            for (var i = 0; i < x; i++) {
                this.newManager.add(this.randCallbacks[i][0], this.randCallbacks[i][1], this, this.randCallbacks[i][2]);
            }
            for (var i = 0; i < x; i++) {
                this.priorityManager.add(this.randCallbacks[i][0], this.randCallbacks[i][1], this, this.randCallbacks[i][2]);
            }
        },

        tearDown: function () {
            this.newManager = null;
            this.priorityManager = null;
        },

        afterAll: function () {
        },

        testNewManagerFire: function () {
            this.newManager.fire('click', this.event);
        },

        testPriorityManagerFire: function () {
            this.priorityManager.fire('click', this.event);
        }
    });
});
