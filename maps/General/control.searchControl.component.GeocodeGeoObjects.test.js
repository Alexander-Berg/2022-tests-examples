ymaps.modules.define(util.testfile(), [
    'control.searchControl.component.GeocodeGeoObjects',
    'control.SearchControl',
    'Map'
], function (provide, GeocodeGeoObjectsComponent, SearchControl, Map) {

    describe('control.searchControl.component.GeocodeGeoObjects', function () {

        var control,
            component,
            myMap,
            MAP_PROPERTIES = {
                center: [55.777153093859496, 37.639130078124964],
                zoom: 10,
                controls: [],
                type: null
            };

        var fakeGeocodeProvider = util.mocha.ymaps.module({
            name: 'test.control.searchControl.fakeGeocodeProvider',
            url: '/src/control/search/provider/geocode/test.control.searchControl.fakeGeocodeProvider.js'
        });

        before(function () {
            myMap = new Map('map', MAP_PROPERTIES);
        });

        after(function () {
            myMap.destroy();
            myMap = null;
        });

        beforeEach(function () {
            control = new SearchControl({
                state: {noSuggestPanel: true},
                options: {provider: fakeGeocodeProvider.module}
            });
            myMap.controls.add(control);
            component = new GeocodeGeoObjectsComponent(control);
        });

        afterEach(function () {
            myMap.controls.remove(control);
            control.destroy();
            component.destroy();
        });

        it('Должен верно отработать состояния инициализации в зависимости от того, добавлен ли контрол на карту', function () {
            myMap.controls.remove(control);
            var myComponent = new GeocodeGeoObjectsComponent(control);

            expect(myComponent.isInited()).to.be(false);
            myMap.controls.add(control);

            expect(myComponent.isInited()).to.be(true);
            myComponent.destroy();
        });

        it('Должен показать один объект на карте', function (done) {
            control.search('Москва').done(function () {
                component.show(0).done(function () {
                    var collection = component.getCollection(),
                        geoObject = collection.get(0);

                    expect(collection.getLength()).to.be(1);
                    expect(geoObject.getMap()).to.eql(myMap);
                    done();
                });
            });
        });

        it('Должен убрать объект с карты', function (done) {
            control.search('Москва').done(function () {
                component.show(0).then(function () {
                    component.hide();
                    expect(component.getCollection().getLength()).to.be(0);
                    done();
                });
            });
        });

        it('Должен корректно показать объект при переносе контрола на другую карту', function (done) {
            var tempContainer = document.createElement('div');
            tempContainer.style.display = 'none';
            document.body.appendChild(tempContainer);

            var tempMap = new Map(tempContainer, MAP_PROPERTIES);

            control.search('Москва').done(function () {
                component.show(0).done(function () {
                    myMap.controls.remove(control);
                    tempMap.controls.add(control);

                    var collection = component.getCollection(),
                        geoObject = collection.get(0);

                    expect(collection.getLength()).to.be(1);
                    expect(geoObject.getMap()).to.eql(tempMap);

                    tempMap.controls.remove(control);
                    myMap.controls.add(control);

                    tempMap.destroy();
                    tempContainer.parentNode.removeChild(tempContainer);

                    done();
                });
            });
        });
    });

    provide();
});
