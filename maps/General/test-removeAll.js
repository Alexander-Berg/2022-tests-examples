define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'RemoveAll',

        axes: ['Listeners'],

        beforeAll: function () {
            this.eventManagerMock = {
                add: function () {},
                remove: function () {}
            };
        },

        setUp: function (x) {
            this.oldGroup = new ym2_0.event.ArrayGroup(this.eventManagerMock);
            for (var i = 0; i < x; i++) {
                this.oldGroup.add('click' + i, function () {}, this);
            }
            this.newGroup = new ymaps.event.Group(this.eventManagerMock);
            for (var i = 0; i < x; i++) {
                this.newGroup.add('click' + i, function () {}, this);
            }
        },

        tearDown: function (x) {
            this.oldGroup = null;
            this.newGroup = null;
        },

        afterAll: function () {
        },

        testOldRemoveAll: function (x) {
            this.oldGroup.removeAll();
        },

        testNewRemoveAll: function (x) {
            this.newGroup.removeAll();
        }
    });
});
