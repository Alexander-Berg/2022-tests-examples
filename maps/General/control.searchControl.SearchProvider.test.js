ymaps.modules.define(util.testfile(), [
    'control.searchControl.SearchProvider',
    'control.SearchControl',
    'yandex.searchProvider.storage',
    'Map'
], function (provide, SearchProvider, SearchControl, searchProviderStorage, Map) {

    var control,
        provider,
        myMap;

    describe('control.searchControl.SearchProvider', function () {
        var fakeSearchProvider = util.mocha.ymaps.module({
            name: 'test.control.searchControl.fakeSearchProvider',
            url: '/src/control/search/provider/search/test.control.searchControl.fakeSearchProvider.js'
        });

        before(function () {
            searchProviderStorage.add('yandex#fakeSearch', fakeSearchProvider.module);

            myMap = new Map('map', {
                center: [55.777153093859496, 37.639130078124964],
                zoom: 10,
                controls: [],
                type: null
            });
        });

        after(function () {
            searchProviderStorage.remove('yandex#fakeSearch');
            myMap.destroy();
        });

        beforeEach(function () {
            control = new SearchControl({
                options: {provider: 'yandex#fakeSearch'}
            });
            provider = new SearchProvider(control);
            myMap.controls.add(control);
        });

        afterEach(function () {
            myMap.controls.remove(control);
            provider.destroy();
            control.destroy();
        });

        describe('Функциональность', function () {
            it('Должен загрузить данные и проверить, что они получены', function (done) {
                this.timeout(10000);
                provider.load('банк').then(function () {
                    var geoObjects = provider.getResults();
                    expect(geoObjects).to.not.be.empty();
                    done();
                });
            });

            it('Должен произвести поиск и вернуть первый результат', function (done) {
                this.timeout(10000);
                provider.load('банк').then(function () {
                    expect(provider.getResult(0)).to.not.be.empty();
                    done();
                });
            });

            it('Должен произвести поиск и вернуть последний результат', function (done) {
                this.timeout(10000);
                provider.load('банк').then(function () {
                    var found = provider.getResponseMetaData().SearchResponse.found,
                        loaded = provider.getResults().length;

                    provider.load({results: found - loaded, type: 'loadmore'}).then(function (res) {
                        expect(provider.getResult(found - 1)).to.not.be.empty();
                        done();
                    });
                });
            });

            it('Должен правильно определить наличие саджеста', function (done) {
                this.timeout(10000);
                control.options.set('provider', 'yandex#search');
                provider.hasSuggest().then(function (hasSuggest) {
                    expect(hasSuggest).to.be(true);
                    control.options.unset('provider');
                    done();
                });
            });

            it('Должен правильно определить отсутствие саджеста', function (done) {
                this.timeout(10000);
                provider.hasSuggest().then(function (hasSuggest) {
                    expect(hasSuggest).to.be(false);
                    control.options.unset('provider');
                    done();
                });
            });

            it('Должен правильно определить наличие саджеста у кастомного провайдера', function (done) {
                this.timeout(10000);
                control.options.set('provider', {
                    search: function () {},
                    suggest: function () {}
                });
                provider.hasSuggest().then(function (hasSuggest) {
                    expect(hasSuggest).to.be(true);
                    control.options.unset('provider');
                    done();
                });
            });

            it('Должен правильно определить отсутствие саджеста у кастомного провайдера', function (done) {
                this.timeout(10000);
                control.options.set('provider', {
                    search: function () {}
                });
                provider.hasSuggest().then(function (hasSuggest) {
                    expect(hasSuggest).to.be(false);
                    control.options.unset('provider');
                    done();
                });
            });
        });

        describe('События', function () {
            this.timeout(10000);
            it('Должен прокинуть событие @submit при поиске', function (done) {
                provider.events
                    .add('submit', function () {
                        done();
                    });

                provider.load('банк');
            });

            it('Должен прокинуть событие @load при поиске', function (done) {
                provider.events
                    .add('load', function () {
                        done();
                    });

                provider.load("банк");
            });
        });
    });

    provide();
});
