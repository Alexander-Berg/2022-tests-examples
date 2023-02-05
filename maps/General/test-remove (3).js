define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Remove',

        axes: ['Listeners'],

        beforeAll: function (x) {
            this.callbacks = [];
            for (var i = 0; i < x; i++) {
                this.callbacks[i] = ['click' + i, function () {}];
            }
            // Перемешиваем колбеки для удаления в случайном порядке.
            this.randCallbacks = [];
            var callbacksCopy = this.callbacks.slice();
            for (var i = 0; i < x; i++) {
                this.randCallbacks.push(callbacksCopy.splice(Math.floor(Math.random() * callbacksCopy.length), 1)[0]);
            }

            this.eventManagerMock = {
                add: function () {},
                remove: function () {}
            };
        },

        setUp: function (x) {
            this.oldGroup = new ym2_0.event.ArrayGroup(this.eventManagerMock);
            this.newGroup = new ymaps.event.Group(this.eventManagerMock);

            for (var i = 0; i < x; i++) {
                this.oldGroup.add(this.callbacks[i][0], this.callbacks[i][1], this);
            }
            for (var i = 0; i < x; i++) {
                this.newGroup.add(this.callbacks[i][0], this.callbacks[i][1], this);
            }
        },

        tearDown: function (x) {
            this.oldGroup = null;
            this.newGroup = null;
        },

        afterAll: function (x) {
        },

        testOldManagerRemove: function (x) {
            for (var i = 0; i < x; i++) {
                this.oldGroup.remove(this.randCallbacks[i][0], this.randCallbacks[i][1], this);
            }
        },

        testNewManagerRemove: function (x) {
            for (var i = 0; i < x; i++) {
                this.newGroup.remove(this.randCallbacks[i][0], this.randCallbacks[i][1], this);
            }
        }
    });
});
