define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Add',

        axes: ['Listeners'],

        beforeAll: function (x) {
            this.callbacks = [];
            for (var i = 0; i < x; i++) {
                this.callbacks[i] = ['click', function () {}];
            }

            this.eventManagerMock = {
                add: function () {},
                remove: function () {}
            };
        },

        setUp: function (x) {
            this.oldGroup = new ym2_0.event.ArrayGroup(this.eventManagerMock);
            this.newGroup = new ymaps.event.Group(this.eventManagerMock);
        },

        tearDown: function (x) {
            this.oldGroup = null;
            this.newGroup = null;
        },

        afterAll: function (x) {
        },

        testOldGroupAdd: function (x) {
            for (var i = 0; i < x; i++) {
                this.oldGroup.add(this.callbacks[i][0], this.callbacks[i][1], this);
            }
        },

        testOldGroupAddI: function (x) {
            for (var i = 0; i < x; i++) {
                this.oldGroup.add(this.callbacks[i][0] + i, this.callbacks[i][1], this);
            }
        },

        testNewGroupAdd: function (x) {
            for (var i = 0; i < x; i++) {
                this.newGroup.add(this.callbacks[i][0], this.callbacks[i][1], this);
            }
        },

        testNewGroupAddI: function (x) {
            for (var i = 0; i < x; i++) {
                this.newGroup.add(this.callbacks[i][0] + i, this.callbacks[i][1], this);
            }
        }
    });
});
