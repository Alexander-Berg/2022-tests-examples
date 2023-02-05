ymaps.modules.define(util.testfile(), [
    'map.action.Single'
], function (provide, SingleAction) {
    describe('map.action.Single', function () {
        var map;
        beforeEach(function () {
            map = new ymaps.Map(util.createMapContainer(), {
                center: [54, 39],
                type: null,
                zoom: 7
            });
        });

        afterEach(function () {
            util.destroyMapAndContainer(map);
        });

        it('should allow to call end() before zoom range callback', function (done) {
            var action = new SingleAction({
                center: [0, 0],
                zoom: 20,
                duration: 0,
                checkZoomRange: true,
                callback: function(err) { done(err); }
            });

            map.action.execute(action);
            action.end();
        });

        it('should work', function (done) {
            this.timeout(4000);

            map.action.execute(new SingleAction({
                center: [0, 0],
                zoom: 4,
                duration: 1000,
                timingFunction: 'ease-in',
                checkZoomRange: true,
                callback: function(err) { done(err); }
            }));
        });

        it('should immediately update center', function () {
            map.action.execute(new SingleAction({
                center: [0, 0],
                timingFunction: 'ease-in'
            }));

            expect(map.getCenter()).to.be.coordinates([0, 0]);
            expect(map.getZoom()).to.be(7);
        });

        it('should allow to reuse actions on different maps', function () {
            var action = new SingleAction({
                center: [0, 0],
                timingFunction: 'ease-in'
            });

            map.action.execute(action);

            var newMap = new ymaps.Map(util.createMapContainer(), {
                center: [54, 39],
                type: null,
                zoom: 5
            });

            newMap.action.execute(action);

            expect(newMap.getCenter()).to.be.coordinates([0, 0]);
            expect(newMap.getZoom()).to.be(5);

            util.destroyMapAndContainer(newMap);
        });
    });

    provide({});
})
