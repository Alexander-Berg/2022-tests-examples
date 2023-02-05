ymaps.modules.define(util.testfile(), [
    'data.Manager',
    'inception.linkBuilder.androidMaps',
    'localization.common.current',
    'util.array'
], function (provide, DataManager, linkBuilder, currentLocalization, utilArray) {

    var mapsLink = currentLocalization.distribution.yaMapsAndroidLink;

    describe('test.yandex.state.androidMapsLinkBuilder', function () {
        var dataStore;
        beforeEach(function () {
            dataStore = new DataManager();
        });

        it('Должен сгенерировать ссылку без параметров', function () {
            var emptyLink = linkBuilder.build(dataStore.getAll());
            expect(emptyLink).to.be(
                mapsLink
                    .replace('{parameters}', '?')
                    .replace('{fallback_url}', 'https://l7test.yandex.ru/maps/?')
            );
        });

        it('Должен сгенерировать ссылку с одним параметром (уровень масштабирования)', function () {
            dataStore.set('map', {'zoom': 14});
            var link = linkBuilder.build(dataStore.getAll());

            expect(test(link, [
                ['z', '14']
            ])).to.be(true);
        });

        it('Должен сгенерировать ссылку с несколькими параметрами', function () {
            dataStore.set('map', {
                zoom: 14,
                center: [11, 11]
            });

            var link = linkBuilder.build(dataStore.getAll());

            expect(test(link, [
                ['ll', '11,11'],
                ['z', '14']
            ])).to.be(true);
        });
    });

    function test (link, params) {
        var linkParams = link.split('?')[1].split('#Intent')[0];
        var linkData = {};
        var testResult = true;

        linkParams = linkParams.split('&');
        if (linkParams.length != params.length) {
            return false;
        }
        utilArray.each(linkParams, function (param) {
            param = param.split('=');
            linkData[param[0]] = param[1];
        });
        utilArray.each(params, function (param) {
            if (typeof linkData[param[0]] == 'undefined' || linkData[param[0]] !== param[1]) {
                testResult = false;
            }
        });
        return testResult;
    }

    provide();
});
