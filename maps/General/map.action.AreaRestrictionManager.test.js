ymaps.modules.define(util.testfile(), [
    'map.action.AreaRestrictionManager',
    'Map',
    'util.math.areEqual',
    'expect'
], function (provide, AreaRestrictionManager, Map, areEqual) {
    describe('map.action.AreaRestrictionManager', function () {

        var myMap;

        beforeEach(function () {
            myMap = new Map('map', {
                center: [55.777153093859496, 37.639130078124964],
                zoom: 10,
                controls: [],
                type: null
            });
        });

        afterEach(function () {
            myMap.destroy();
        });

        it('Центр карты (координаты) не должен измениться при включенной опции restrictMapArea', function () {
            myMap.options.set({
                restrictMapArea: true,
                avoidFractionalZoom: false
            });

            var center = myMap.getCenter();
            myMap.setCenter([center[0] + 1, center[1] + 2]);

            expect(areEqual(center, myMap.getCenter(), 1e-5)).to.be.ok();
        });

        it('Центр карты (пиксели) не должен измениться при включенной опции restrictMapArea', function () {
            myMap.options.set({
                restrictMapArea: true,
                avoidFractionalZoom: false
            });

            var pixelCenter = myMap.getGlobalPixelCenter();
            myMap.setGlobalPixelCenter([pixelCenter[0] + 400, pixelCenter[1] + 400]);

            expect(areEqual(pixelCenter, myMap.getGlobalPixelCenter(), 2)).to.be.ok();
        });

        it('Зум карты не должен измениться при включенной области, равной текущему bounds карты', function () {
            myMap.options.set('restrictMapArea', true);

            var mapZoom = myMap.getZoom();
            myMap.setZoom(4);
            expect(Math.abs(mapZoom - myMap.getZoom()) < 1e-4);
        });
    });

    provide();
});
