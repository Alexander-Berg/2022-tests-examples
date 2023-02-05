ymaps.modules.define(util.testfile(), [
    'Map', 'Placemark', 'geoObject.Balloon', 'util.dom.element'
], function (provide, Map, Placemark, GeoObjectBalloon, domElement) {
    describe('geoObject.Balloon', function () {
        this.timeout(10000);

        var map, geoObject, geoObjectBalloon;

        beforeEach(function () {
            map = new Map('map', {
                center: [0, 0],
                type: null,
                zoom: 3,
                controls: []
            });
            geoObject = new Placemark(map.getCenter(), {
                balloonContent: 'Balloon'
            });
            map.geoObjects.add(geoObject);
            geoObjectBalloon = new GeoObjectBalloon(geoObject);
        });

        afterEach(function () {
            geoObjectBalloon.destroy();
            map.destroy();
        });

        it('Метод open должен открывать балун', function (done) {
            geoObjectBalloon.open().done(function () {
                expect(domElement.findByPrefixedClass(map.container.getElement(), 'balloon-overlay')).to.be.ok();
                done();
            });
        });

        it('should close silently after geoobject is removed from the map', function (done) {
            geoObjectBalloon.open().done(function () {
                expect(geoObjectBalloon.isOpen()).to.be(true);
                expect(geoObject.state.get('active')).to.be(true);
                map.geoObjects.remove(geoObject);
                setTimeout(function () {
                    expect(geoObjectBalloon.isOpen()).to.not.be(true);
                    expect(geoObject.state.get('active')).to.not.be(true);
                    done();
                }, 500);
            });
        });

        it('should not open if geoobject is removed from the map', function (done) {
            geoObjectBalloon.open().then(function () {
                done('Open promise was resolved');
            }, function () {
                expect(geoObjectBalloon.isOpen()).to.not.be(true);
                expect(geoObject.state.get('active')).to.not.be(true);
                done();
            }, this);
            map.geoObjects.remove(geoObject);
        });

        describe('events', function () {
            this.timeout(500);

            it('should fire `close` when geoobject is removed from map', function (done) {
                geoObjectBalloon.events.add('close', function () { done(); });
                geoObjectBalloon.open().then(function () {
                    map.geoObjects.remove(geoObject);
                }, this);
            });
        });

        describe('autoPan', function () {
            it('Должен перемещать карту', function (done) {
                var coords = geoObject.geometry.getCoordinates();
                map.setCenter(coords, 15);
                geoObjectBalloon.open().done(function () {
                    map.setCenter([coords[0] + 20, coords[1] + 20]);

                    geoObjectBalloon.autoPan().done(function () {
                        var center = map.getCenter();

                        expect(areSimilar(center[0], coords[0], 2)).to.be.ok();
                        expect(areSimilar(center[1], coords[1], 2)).to.be.ok();
                        done();
                    });
                });
            });
        });
    });

    provide({});
});

function areSimilar (a, b, accuracy) {
    return Math.abs(a - b) <= Math.pow(10, -(typeof accuracy == 'number' ? accuracy : 3));
}
