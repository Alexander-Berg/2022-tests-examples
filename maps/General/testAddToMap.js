define(['jsSpeedTest/test-case'], function (TestCase) {
    var timeout;

    return new TestCase({
        name: 'Add',

        axes: ['GeoObjects'],

        beforeAll: function (x) {
        },

        setUp: function (x) {
        },

        tearDown: function (x) {
        },

        afterAll: function (x) {
            window.clearTimeout(timeout);
            timeout = window.setTimeout(function () {
                map.destroy();
            }, 50);
        },

        testAdd: function (x) {
            for (var i = 0; i < x; i++) {
                map.geoObjects.add(
                    new ymaps.GeoObject({
                        geometry: {
                            type: 'Point',
                            coordinates: [0, 0]
                        }
                    })
                );
            }
        }
    });
});
