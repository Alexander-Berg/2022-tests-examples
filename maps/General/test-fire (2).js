define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Fire',

        axes: [],

        beforeAll: function () {
            this.event = new ymaps.Event();
        },

        setUp: function () {
            this.oldManager = new oldYMaps.BaseOldManager();
            this.oldManager.add('click', function () {}, this);

            this.newManager = new ymaps.event.manager.Base();
            this.newManager.add('click', function () {}, this);

            this.priorityManager = new ymaps.event.manager.Base();
            this.priorityManager.add('click', function () {}, this);
        },

        tearDown: function () {
            this.oldManager = null;
            this.newManager = null;
            this.priorityManager = null;
        },

        afterAll: function () {
        },

        testOldManagerFire: function () {
            this.oldManager.fire('click', this.event);
        },

        testNewManagerFire: function () {
            this.newManager.fire('click', this.event);
        },

        testPriorityManagerFire: function () {
            this.priorityManager.fire('click', this.event);
        }
    });
});
