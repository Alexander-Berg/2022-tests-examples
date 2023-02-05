ymaps.modules.define(util.testfile(), [
    'yandex.State',
    'Map',
    'Placemark',
    'yandex.state.associate'
], function (provide, YandexState, Map, Placemark, yandexStateAssociate) {

    describe('yandex.State', function () {
        var yState,
            map;

        before(function () {
            map = new Map('map', {
                type: null,
                center: [55.755768, 37.617671],
                zoom: 13
            });
        });

        after(function () {
            map.destroy();
        });

        beforeEach(function () {
            yState = new YandexState(map);
        });

        it('должен кинуть событие изменения данных', function (done) {
            var zoom = 12;
            yState.events.once('change', function () {
                done();
            });

            yState.setMapState({
                center: [1,1],
                zoom: zoom
            });
        });

        it('Должен корректно отработать при прореженной коллекции геообъектов', function (done) {
            yState = yandexStateAssociate.get(map);
            yState.events.add('change', function () {
                expect(yState.get('points').length).to.eql(2);
                map.geoObjects.removeAll();
                done();
            });

            map.geoObjects
                .add(new Placemark([55.590139, 37.814052]), 2)
                .add(new Placemark([55.690139, 37.814052]), 10);
        });
    });

    provide();
});
