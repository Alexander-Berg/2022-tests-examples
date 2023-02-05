ymaps.modules.define(util.testfile(), [
    'Map',
    'traffic.AutoUpdater',
    'MapEvent',
    'expect'
], function (provide, Map, AutoUpdater, MapEvent, expect) {
    var map;

    describe('traffic.AutoUpdater', function () {
        beforeEach(function () {
            map = new Map('map', { center: [37.621587,55.74954], zoom: 10, behaviors: [], controls: [], type: null});
        });

        afterEach(function () {
            map.destroy();
        });

        it('Должен вызвать сообщение об обновлении данных после mousemove', function (done) {
            var res = "",
                testCallback = function() {
                    res = res + '1';
                },
                updater = new AutoUpdater(100, testCallback);

            updater.enable(map);
            var fireMove = function() {
                map.events.fire('mousemove', new MapEvent({
                    target: this,
                    map: map
                }));
                expect(res).to.be('1');
                done();
            };
            window.setTimeout(fireMove, 500);
        });

        it('Не должен кинуть событие после вызова disable', function (done) {
            var res2 = '',
                testCallback2 = function() {
                    res2= res2 + '1';
                },
                updater = new AutoUpdater(100, testCallback2);
            updater.enable(map);
            updater.disable();
            var fireMove = function() {
                map.events.fire('mousemove', new MapEvent({
                    target: this,
                    map: map
                }));
                expect(res2).to.be('');
                done();
            };
            window.setTimeout(fireMove, 500);
        });
    });

    provide({});
});
