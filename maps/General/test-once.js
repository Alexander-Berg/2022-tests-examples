define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Once',

        axes: [],

        beforeAll: function () {
            this.type = 'click';
            this.listener = function (e) {
                console.log(e);
            };
        },

        setUp: function () {
            this.manager = new ymaps.event.manager.Base();
        },

        tearDown: function () {
        },

        afterAll: function () {
        },

        testOnce: function () {
            this.manager.once(this.type, this.listener, this);
        },

        testUtilOnce: function () {
            oldYMaps.utilOnce(this.manager, this.type, this.listener, this);
        }
    });
});
