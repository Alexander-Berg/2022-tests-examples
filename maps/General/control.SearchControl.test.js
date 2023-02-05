ymaps.modules.define(util.testfile(), [
    'control.SearchControl',
    'yandex.searchProvider.storage',
    'Map'
], function (provide, SearchControl, searchProviderStorage, Map) {

    var FAKE_SEARCH_PROVIDER_NAME = 'fakeSearch#test.control.SearchControl',
        myMap;

    describe('control.SearchControl', function () {
        var fakeGeocodeProvider = util.mocha.ymaps.module({
            name: 'test.control.searchControl.fakeGeocodeProvider',
            url: '/src/control/search/provider/geocode/test.control.searchControl.fakeGeocodeProvider.js'
        });
        var fakeSearchProvider = util.mocha.ymaps.module({
            name: 'test.control.searchControl.fakeSearchProvider',
            url: '/src/control/search/provider/search/test.control.searchControl.fakeSearchProvider.js'
        });

        before(function () {
            myMap = new Map('map', {
                center: [55.777153093859496, 37.639130078124964],
                zoom: 10,
                controls: [],
                type: null
            });
        });

        after(function (done) {
            setTimeout(function () {
                myMap.destroy();
                myMap = null;
                done();
            }, 20);
        });

        var jsonp = util.mocha.mock.jsonp();
        beforeEach(function () {
            // yandex.timeZone
            jsonp.mock.stub(/\/services\/coverage\/v2\/\?.*\bl=trf,trfa\b/)
                .completeWith({"status":"success","data":[{"id":"trf","zoomRange":[0,23],"LayerMetaData":[{"geoId":213,"archive":true,"events":true},{"geoId":1,"archive":true,"events":true}]},{"id":"trfa","LayerMetaData":[{"geoId":213,"offset":10800,"dst":"std"},{"geoId":1,"offset":10800,"dst":"std"}]}]})
                .play();
        });

        describe('Geocode provider', function () {
            var searchControl;

            // setup.
            beforeEach(function () {
                this.timeout(10000);
                searchControl = new SearchControl({
                    options: {
                        size: 'large',
                        float: 'none',
                        provider: fakeGeocodeProvider.module
                    }
                });
                myMap.controls.add(searchControl);
            });

            // teardown.
            afterEach(function () {
                myMap.controls.remove(searchControl);
                searchControl.destroy();
            });

            // Тестирование.
            it('Должен произвести поиск и вернуть хотя бы один результат', function (done) {
                this.timeout(10000);
                searchControl.search('Москва').done(function () {
                    var geoObjects = searchControl.getResultsArray();
                    expect(geoObjects).to.not.be.empty();
                    done();
                });
            });

            it('Должен произвести поиск и получить первый результат', function (done) {
                this.timeout(10000);
                searchControl.search('Москва').done(function () {
                    searchControl.getResult(0).done(function (geoObject) {
                        expect(geoObject).to.not.be.empty();
                        done();
                    });
                });
            });

            it('Должен произвести поиск и вернуть последний результат', function (done) {
                this.timeout(10000);
                searchControl.search("Москва").done(function () {
                    var found = searchControl.getResultsCount();
                    searchControl.getResult(found - 1).done(function (geoObject) {
                        expect(geoObject).to.not.be.empty();
                        done();
                    });
                });
            });

            it('Должен верно отработать поиск с обратным порядком координат', function (done) {
                this.timeout(10000);
                searchControl.options.set('searchCoordOrder', 'longlat');
                searchControl.search('84.70126567, 88.20093371').done(function () {
                    var arr = searchControl.getResultsArray();
                    expect(arr).to.have.length(1);
                    done();
                });
            });

            it('Должен верно вернуть индекс выбранного элемента', function () {
                this.timeout(10000);
                return searchControl.search('Москва')
                    .then(function () {
                        return searchControl.showResult(1);
                    })
                    .then(function () {
                        expect(searchControl.getSelectedIndex()).to.be(1);
                    });
            });
        });

        describe('Search provider', function () {
            var searchControl;

            before(function () {
                searchProviderStorage.add(FAKE_SEARCH_PROVIDER_NAME, fakeSearchProvider.module);
            });

            after(function () {
                searchProviderStorage.remove(FAKE_SEARCH_PROVIDER_NAME);
            });

            beforeEach(function () {
                searchControl = new SearchControl({
                    options: {
                        provider: FAKE_SEARCH_PROVIDER_NAME,
                        size: 'large',
                        float: 'none'
                    },
                    state: {noSuggestPanel: true}
                });

                myMap.controls.add(searchControl);
            });

            afterEach(function () {
                myMap.controls.remove(searchControl);
                searchControl.destroy();

                searchControl = null;
            });

            // Тестирование.
            it('Должен произвести поиск и вернуть хотя бы один результат', function (done) {
                this.timeout(10000);
                searchControl.search('банк').done(function () {
                    var geoObjects = searchControl.getResultsArray();
                    expect(geoObjects).to.not.be.empty();
                    done();
                });
            });

            it('Должен произвести поиск и получить первый результат', function (done) {
                this.timeout(10000);
                searchControl.search('банк').done(function () {
                    searchControl.getResult(0).done(function (geoObject) {
                        expect(geoObject).to.not.be.empty();
                        done();
                    });
                });
            });

            it('Должен произвести поиск и вернуть последний результат', function (done) {
                this.timeout(10000);
                searchControl.search('банк').done(function () {
                    var found = searchControl.getResultsCount();
                    setTimeout(function () {
                        searchControl.getResult(found - 1).done(function (geoObject) {
                            expect(geoObject).to.not.be.empty();
                            done();
                        });
                    }, 50);
                });
            });

            it('Должен верно вернуть индекс выбранного элемента', function () {
                this.timeout(10000);
                return searchControl.search('банк')
                    .then(function () {
                        return searchControl.showResult(1);
                    })
                    .then(function () {
                        expect(searchControl.getSelectedIndex()).to.be(1);
                    });
            });
        });

        // layout.
        describe('Тестирование лейаута контрола', function () {
            var searchControl;

            // setup.
            beforeEach(function () {
                this.timeout(10000);
                searchControl = new SearchControl();
                myMap.controls.add(searchControl, {
                    size: 'large',
                    float: 'none',
                    provider: fakeGeocodeProvider.module
                });
            });

            // teardown.
            afterEach(function () {
                myMap.controls.remove(searchControl);
            });

            describe('Методы и состояния', function () {
                it('Должен открыть попап с результатами поиска и изменить состояние', function (done) {
                    this.timeout(10000);
                    searchControl.getLayout().done(function (layout) {
                        layout.closePopup();
                        var popupState = layout.state.get('popupOpened');
                        layout.openPopup();

                        expect(popupState).to.not.be(layout.state.get('popupOpened'));
                        done();
                    });
                });

                it('Должен закрыть попап с результатами поиска и изменить состояние', function (done) {
                    this.timeout(10000);
                    searchControl.getLayout().done(function (layout) {
                        layout.openPopup();
                        var popupState = layout.state.get('popupOpened');
                        layout.closePopup();

                        expect(popupState).to.not.be(layout.state.get('popupOpened'));
                        done();
                    });
                });

                it('Должен открыть панель с контролом поиска и изменить состояние', function (done) {
                    this.timeout(10000);
                    searchControl.getLayout().done(function (layout) {
                        layout.closePanel();
                        var panelState = layout.state.get('panelOpened');
                        layout.openPanel();

                        expect(panelState).to.not.be(layout.state.get('panelOpened'));
                        done();
                    });
                });

                it('Должен закрыть панель с контролом поиска и изменить состояние', function (done) {
                    this.timeout(10000);
                    searchControl.getLayout().done(function (layout) {
                        layout.openPanel();
                        var panelState = layout.state.get('panelOpened');
                        layout.closePanel();

                        expect(panelState).to.not.be(layout.state.get('panelOpened'));
                        done();
                    });
                });
            }); // layout.methods

            describe('События', function () {
                it('Должно сработать событие @submit', function (done) {
                    this.timeout(10000);
                    var controlListener = searchControl.events.group()
                        .add('submit', function () { done(); });

                    searchControl.search("Москва").done();
                });

                it('Должно сработать событие @load', function (done) {
                    this.timeout(10000);
                    var controlListener = searchControl.events.group()
                        .add('load', function () { done(); });

                    searchControl.search('Москва').done();
                });

                it('Должно сработать событие @resultshow', function (done) {
                    this.timeout(10000);
                    var controlListener = searchControl.events.group()
                        .add('resultshow', function () {
                            // Здесь нужен таймаут, иначе возникает ошибка при асинхронной загрузке модуля оверлея балуна.
                            setTimeout(function () {
                                done();
                            }, 1000);
                        });

                    searchControl.options.set('noSelect', true);
                    searchControl.search("Москва").then(function () {
                        searchControl.showResult(0);
                    }).done();
                });
            }); // events
        }); // layout
    });

    provide();
});
