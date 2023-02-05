ymaps.modules.define(util.testfile(), [
    'Map',
    'util.math.areEqual'
], function (provide, Map, areEqual) {
    describe('map.action.Correction', function () {

        var map;
        var correction;
        var mapCenter;

        beforeEach(function () {
            map = new Map('map', {
                center: [55.777153093859496, 37.639130078124964],
                zoom: 10,
                controls: [],
                type: null
            });

            mapCenter = map.getCenter();

            correction = function (tick) {
                tick.globalPixelCenter = map.options.get('projection').toGlobalPixels(mapCenter, tick.zoom);
                return tick;
            };
        });

        afterEach(function () {
            map.destroy();
        });

        it('Позволяет установить действие коррекции', function () {
            map.action.setCorrection(correction);
            map.setCenter([60, 40]);
            expect(areEqual(mapCenter, map.getCenter(), 1e-7)).to.be.ok();
        });

        it('Позволяет удалить действие коррекции', function () {
            map.action.setCorrection(correction);
            map.action.setCorrection(null);
            map.setCenter([55, 45]);
            expect(areEqual(map.getCenter(), [55, 45], 1e-7)).to.be.ok();
        });
    });

    provide();
});
