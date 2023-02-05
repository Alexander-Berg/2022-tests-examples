ymaps.modules.define(util.testfile(), [
    'objectManager.component.ObjectController',
    'Map',
    'option.Manager',
    'system.browser'
], function (provide, ObjectController, Map, OptionManager, browser) {

    describe('ObjectController.speed', function () {
        var map,
            center = [37.621587, 55.74954],
            objectController,
            coordinates = generateRandomCoordinates(10000),
            features;

        beforeEach(function () {
            map = new Map('map', {
                center: center,
                zoom: 10,
                controls: [],
                type: null
            });
            objectController = new ObjectController(new OptionManager);
            features = generateFeaturesArray(coordinates);
        });

        afterEach(function () {
            map.destroy();
            map = null;
            objectController.destroy();
            objectController = null;
        });

        if (!window.mochaPhantomJS) {
            it('Должен добавить в контроллер 10 000 объектов менее, чем за 200мс', function () {
                var startTime = +new Date();
                objectController.setMap(map);
                objectController.add(features);
                var stopTime = +new Date();
                expect(stopTime - startTime).to.be.lessThan(200);
            });
        }

        it('Должен удалить половину объектов из контроллера менее, чем за 200мс', function () {
            objectController.setMap(map);
            objectController.add(features);
            features.length = 5000;
            var startTime = +new Date();
            objectController.remove(features);
            var stopTime = +new Date();
            expect(stopTime - startTime).to.be.lessThan(200);
        });

        function generateRandomCoordinates (N) {
            var coords = [];
            for (var i = 0; i < N; i++) {
                var x = center[0] + 0.5 * Math.random() * Math.random() * Math.random() * Math.random() * (Math.random() < 0.5 ? -1 : 1),
                    y = center[1] + 0.7 * Math.random() * Math.random() * Math.random() * Math.random() * (Math.random() < 0.5 ? -1 : 1);
                coords[i] = [x, y];
            }
            return coords;
        }

        function generateFeaturesArray (coords) {
            features = [];
            for (var i = 0, l = coords.length; i < l; i++) {
                features.push({
                    type: 'Features',
                    geometry: {
                        type: 'Point',
                        coordinates: coords[i]
                    },
                    id: i
                });
            }
            return features;
        }
    });

    provide({});
});
