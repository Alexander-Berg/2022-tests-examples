ymaps.modules.define(util.testfile(), [
    'control.searchControl.component.SearchGeoObjects',
    'control.SearchControl',
    'Map',
    'yandex.searchProvider.storage'
], function (provide, SearchGeoObjectsComponent, SearchControl, Map, searchProviderStorage) {

    describe('control.searchControl.component.SearchGeoObjects', function () {

        var control,
            component,
            mapContainer,
            myMap,
            FAKE_SEARCH_PROVIDER_NAME = 'fakeSearch#test.control.searchControl.component.SearchGeoObjects',
            MAP_PROPERTIES = {
                center: [55.777153093859496, 37.639130078124964],
                zoom: 10,
                controls: [],
                type: null
            },
            CONTROL_PARAMETERS = {
                state: {noSuggestPanel: true},
                options: {provider: FAKE_SEARCH_PROVIDER_NAME}
            };

        var fakeSearchProvider = util.mocha.ymaps.module({
            name: 'test.control.searchControl.fakeSearchProvider',
            url: '/src/control/search/provider/search/test.control.searchControl.fakeSearchProvider.js'
        });

        before(function () {
            mapContainer = document.createElement('div');
            mapContainer.style.display = 'none';
            document.body.appendChild(mapContainer);

            searchProviderStorage.add(FAKE_SEARCH_PROVIDER_NAME, fakeSearchProvider.module);
            myMap = new Map(mapContainer, MAP_PROPERTIES);
        });

        after(function () {
            searchProviderStorage.remove(FAKE_SEARCH_PROVIDER_NAME);
            myMap.destroy();
            myMap = null;

            document.body.removeChild(mapContainer);
        });

        var jsonp = util.mocha.mock.jsonp();
        beforeEach(function () {
            // yandex.timeZone
            jsonp.mock.stub(/\/services\/coverage\/v2\/\?.*\bl=trf,trfa\b/)
                .completeWith({"status":"success","data":[{"id":"trf","zoomRange":[0,23],"LayerMetaData":[{"geoId":213,"archive":true,"events":true},{"geoId":1,"archive":true,"events":true}]},{"id":"trfa","LayerMetaData":[{"geoId":213,"offset":10800,"dst":"std"},{"geoId":1,"offset":10800,"dst":"std"}]}]})
                .play();
        });

        beforeEach(function () {
            control = new SearchControl(CONTROL_PARAMETERS);
            myMap.controls.add(control);
            component = new SearchGeoObjectsComponent(control);
        });

        afterEach(function () {
            component.destroy();
            myMap.controls.remove(control);
            control.destroy();
        });

        it('Должен верно отработать состояния инициализации в зависимости от того, добавлен ли контрол на карту', function () {
            myMap.controls.remove(control);
            var myComponent = new SearchGeoObjectsComponent(control);

            expect(myComponent.isInited()).to.be(false);
            myMap.controls.add(control);

            expect(myComponent.isInited()).to.be(true);
            myComponent.destroy();
            myComponent = null;
        });

        it('Должен показать объекты на карте', function (done) {
            this.timeout(10000);
            control.search('банк').done(function () {
                setTimeout(function () {
                    expect(component.getCollection().getLength()).to.not.be(0);
                    done();
                }, 50);
            });
        });

        it('Должен открыть и закрыть балун на карте', function (done) {
            this.timeout(10000);
            control.search('банк').done(function () {
                setTimeout(function () {
                    component.show(0).done(function () {
                        var geoObject = component.getCollection().get(0);

                        expect(geoObject.balloon.isOpen()).to.be.ok();
                        component.hide().then(function () {
                            expect(geoObject.balloon.isOpen()).to.not.be.ok();
                            done();
                        });
                    });
                }, 50);
            });
        });

        it.skip('Должен корректно отобразить объекты при переносе контрола на другую карту', function (done) {
            this.timeout(10000);

            var tempContainer = document.createElement('div');
            tempContainer.style.display = 'none';
            document.body.appendChild(tempContainer);

            var tempMap = new Map(tempContainer, MAP_PROPERTIES);

            control.search('банк').done(function () {
                myMap.controls.remove(control);
                tempMap.controls.add(control);

                var collection = component.getCollection();

                expect(collection.getLength()).to.not.be(0);
                expect(collection.getMap()).to.eql(tempMap);

                tempMap.controls.remove(control);
                myMap.controls.add(control);

                tempMap.destroy();
                document.body.removeChild(tempContainer);

                tempMap = null;
                tempContainer = null;

                done();
            });
        });
    });

    provide();
});
