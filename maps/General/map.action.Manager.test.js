ymaps.modules.define(util.testfile(), [
    'Map', 'map.action.Single'
], function (provide, Map, SingleAction) {
    describe('map.action.Manager', function () {
        var map;

        beforeEach(function () {
            map = window.map = new Map('map', {
                center: [0, 0],
                type: null,
                zoom: 3,
                controls: []
            });
        });

        afterEach(function () {
            map.destroy();
        });

        it('should normally execute action on "end" event of previous one', function (done) {
            var currentCenter = map.getGlobalPixelCenter(),
                intermediateCenter = [currentCenter[0] + 200, currentCenter[1] + 200],
                finalCenter = [currentCenter[0] + 400, currentCenter[1] + 400],
                action1 = new SingleAction({
                    globalPixelCenter: intermediateCenter,
                    zoom: map.getZoom(),
                    duration: 500,
                    timingFunction: 'ease-in-out',
                    checkZoomRange: false
                }, map),
                action2 = new SingleAction({
                    globalPixelCenter: finalCenter,
                    zoom: map.getZoom(),
                    duration: 500,
                    timingFunction: 'ease-in-out',
                    checkZoomRange: false
                }, map);

            map.events.once('actionend', function () {
                map.events.once('actionend', function () {
                    expect(map.getGlobalPixelCenter()).to.eql(finalCenter);
                    done();
                });

                map.action.execute(action2);
            });

            map.action.execute(action1);
        });
    });

    provide({});
});

function areSimilar (a, b, accuracy) {
    return Math.abs(a - b) <= Math.pow(10, -(typeof accuracy == 'number' ? accuracy : 3));
}
