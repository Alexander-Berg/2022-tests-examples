ymaps.modules.define(util.testfile(), [
    'control.searchControl.GeocodeProvider'
], function (provide, SearchControlProvider) {

    var Control,
        control,
        provider;

    describe('control.searchControl.GeocodeProvider', function () {
        var fakeGeocodeProvider = util.mocha.ymaps.module({
            name: 'test.control.searchControl.fakeGeocodeProvider',
            url: '/src/control/search/provider/geocode/test.control.searchControl.fakeGeocodeProvider.js'
        });

        before(function () {
            Control = function () {
                this.events = new ymaps.event.Manager();
                this.options = new ymaps.option.Manager({
                    provider: fakeGeocodeProvider.module
                });
            };
        });

        beforeEach(function () {
            control = new Control();
            provider = new SearchControlProvider(control);
        });

        afterEach(function () {
            provider.destroy();
        });

        describe('Функциональность', function () {
            it('Должен загрузить данные и проверить, что они получены', function (done) {
                this.timeout(10000);
                provider.load('Москва').then(function () {
                    var geoObjects = provider.getResults();
                    expect(geoObjects).to.not.be.empty();
                    done();
                });
            });

            it('Должен произвести поиск и вернуть первый результат', function (done) {
                this.timeout(10000);
                provider.load('Москва').then(function () {
                    expect(provider.getResult(0)).to.not.be.empty();
                    done();
                });
            });

            it('Должен произвести поиск и вернуть последний результат', function (done) {
                this.timeout(10000);
                provider.load('Москва').then(function () {
                    var found = provider.getResponseMetaData().found,
                        loaded = provider.getResults().length;

                    provider.load({results: found - loaded}).then(function () {
                        expect(provider.getResult(found - 1)).to.not.be.empty();
                        done();
                    });
                });
            });

            it('Должен корректно отработать поиск с обратным порядком координат', function (done) {
                this.timeout(10000);
                control.options.set('searchCoordOrder', 'longlat');
                provider.load('84.70126567, 88.20093371').then(function () {
                    var arr = provider.getResults();
                    expect(arr).to.have.length(1);
                    control.options.unset('searchCoordOrder');
                    done();
                });
            });

            it('Должен правильно определить наличие саджеста', function (done) {
                this.timeout(10000);
                control.options.set('provider', 'yandex#map');
                provider.hasSuggest().then(function (hasSuggest) {
                    expect(hasSuggest).to.be(true);
                    control.options.unset('provider');
                    done();
                });
            });

            it('Должен правильно определить отсутствие саджеста', function (done) {
                this.timeout(10000);
                control.options.set('provider', 'yandex#publicMap');
                provider.hasSuggest().then(function (hasSuggest) {
                    expect(hasSuggest).to.be(false);
                    control.options.unset('provider');
                    done();
                });
            });

            it('Должен правильно определить наличие саджеста у кастомного провайдера', function (done) {
                this.timeout(10000);
                control.options.set('provider', {
                    geocode: function () {},
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
                    geocode: function () {}
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

                provider.load('Москва');
            });

            it('Должен прокинуть событие @load при поиске', function (done) {
                provider.events
                    .add('load', function () {
                        done();
                    });

                provider.load("Москва");
            });
        });
    });

    provide();
});
