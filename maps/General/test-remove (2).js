define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Remove',

        axes: ['Listeners'],

        beforeAll: function (x) {
            this.callbacks = [];
            for (var i = 0; i < x; i++) {
                this.callbacks[i] = ['click', function () {}, i];
            }
            // Перемешиваем колбеки для удаления в случайном порядке.
            this.randCallbacks = [];
            var callbacksCopy = this.callbacks.slice();
            for (var i = 0; i < x; i++) {
                this.randCallbacks.push(callbacksCopy.splice(Math.floor(Math.random() * callbacksCopy.length), 1)[0]);
            }
        },

        setUp: function (x) {
            this.oldManager = new oldYMaps.BaseOldManager();
            this.newManager = new ymaps.event.manager.Base();
            this.priorityManager = new oldYMaps.PriorityManager();

            for (var i = 0; i < x; i++) {
                this.oldManager.add(this.callbacks[i][0], this.callbacks[i][1], this);
            }
            for (var i = 0; i < x; i++) {
                this.newManager.add(this.callbacks[i][0], this.callbacks[i][1], this, this.callbacks[i][2]);
            }
            for (var i = 0; i < x; i++) {
                this.priorityManager.add(this.callbacks[i][0], this.callbacks[i][1], this, this.callbacks[i][2]);
            }
        },

        tearDown: function (x) {
            this.oldManager = null;
            this.newManager = null;
            this.priorityManager = null;
        },

        afterAll: function (x) {
        },

        testOldManagerRemove: function (x) {
            for (var i = 0; i < x; i++) {
                this.oldManager.remove(this.randCallbacks[i][0], this.randCallbacks[i][1], this);
            }
        },

        testNewManagerRemove: function (x) {
            for (var i = 0; i < x; i++) {
                this.newManager.remove(this.randCallbacks[i][0], this.randCallbacks[i][1], this, this.randCallbacks[i][2]);
            }
        },

        testPriorityManagerRemove: function (x) {
            for (var i = 0; i < x; i++) {
                this.priorityManager.remove(this.randCallbacks[i][0], this.randCallbacks[i][1], this, this.randCallbacks[i][2]);
            }
        }
    });
});
