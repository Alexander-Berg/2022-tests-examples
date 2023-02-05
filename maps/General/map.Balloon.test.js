ymaps.modules.define(util.testfile(), [
    'Map', 'map.Balloon', 'util.dom.element'
], function (provide, Map, MapBalloon, domElement) {
    describe('map.Balloon', function () {
        var map, mapBalloon;

        beforeEach(function () {
            map = window.map = new Map('map', {
                center: [0, 0],
                type: null,
                zoom: 3,
                controls: []
            });

            mapBalloon = new MapBalloon(map);
        });

        afterEach(function () {
            //TODO: MAPSAPI-12144
            mapBalloon.destroy();
            map.destroy();
        });

        it('Метод open должен открывать балун', function (done) {
            this.timeout(10000);

            mapBalloon.open().then(function () {
                expect(domElement.findByPrefixedClass(map.container.getElement(), 'balloon-overlay')).to.be.ok();
                done();
            });
        });

        describe('autoPan', function () {
            it('Должен перемещать карту', function (done) {
                this.timeout(10000);

                var coords = map.getCenter();

                mapBalloon.open(coords).done(function () {
                    map.setCenter([coords[0] + 20, coords[1] + 20], 15);

                    mapBalloon.autoPan().done(function () {
                        var center = map.getCenter();

                        expect(areSimilar(center[0], coords[0], 2)).to.be.ok();
                        expect(areSimilar(center[1], coords[1], 2)).to.be.ok();
                        done();
                    });
                });
            });

            it('should not begin after map action end', function (done) {
                map.setZoom(6);

                var center = map.getCenter();

                mapBalloon.events.add('autopanbegin', function () {
                    throw new Error('Произошло событие autopanbegin');
                });

                mapBalloon.open(center).done(function () {
                    map.setCenter([center[0] + 20, center[1] + 20], map.getZoom() + 1, { duration: 200 });
                    setTimeout(done, 300);
                });
            });
        });
    });

    provide({});
});

function areSimilar (a, b, accuracy) {
    return Math.abs(a - b) <= Math.pow(10, -(typeof accuracy == 'number' ? accuracy : 3));
}
