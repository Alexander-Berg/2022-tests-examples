define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Fire',

        axes: [],

        beforeAll: function () {
        },

        setUp: function () {
            this.baseManager = new ymaps.event.manager.Base();
            this.baseManager.add('click', function () {}, this);

            this.manager = new ymaps.event.Manager();
            this.manager.add('click', function () {}, this);
        },

        tearDown: function () {
            this.baseManager = null;
            this.manager = null;
        },

        afterAll: function () {
        },

        testManagerFire: function () {
            this.manager.fire('click');
        },

        testBaseManagerFire: function () {
            this.baseManager.fire('click', new ymaps.Event({
                type: 'click',
                target: this
            }));
        }
    });
});
