ymaps.modules.define(util.testfile(), [
    'Map',
    'util.math.areEqual'
], function (provide, Map, areEqual) {
    describe('map.event.simulation', function (done) {
        var map;

        beforeEach(function () {
            map = new Map('map', {
                center: [54, 39],
                type: null,
                zoom: 7
            });
        });

        afterEach(function () {
            map.destroy();
        });

        it('Карта зумится при эмуляции двойного клика', function () {
            map.events.add('boundschange', function () {
                expect(map.getZoom()).to.be(8);
                done();
            });
            map.events.fire('dblclick', { position: [300, 400] });
        });

        it('Балун открывается по клику', function (done) {
            map.events.add('click', function (e) {
                map.balloon.open(e.get('position'))
                    .then(function () {
                        expect(map.balloon.isOpen()).to.be(true);
                        done();
                    });
            });
            map.events.fire('click', { position: [300, 400] });
        });

        it('Карта ловит искуственный mouseenter', function (done) {
            map.events.add('mouseenter', function (e) {
                expect(true).to.be.ok();
                done();
            });
            map.events.fire('mouseenter', { position: [300, 400] });
        });

        provide();
    });
});
