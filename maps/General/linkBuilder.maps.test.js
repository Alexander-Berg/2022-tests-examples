ymaps.modules.define(util.testfile(), [
    'yandex.State',
    'inception.linkBuilder.maps',
    'meta',
    'Map',
    'util.array'
], function (provide, YandexState, linkBuilder, meta, Map, utilArray) {

    var FROM_PARAM = ['from', 'api-maps'];
    var ORIGIN_PARAM = ['origin', 'jsapi_' + meta.version.replace(/\W/g, '_')];
    var getPath = function (url) {
        var match = url.match(/(^http:\/\/|https:\/\/|\/\/)([^\/]+)\/maps\/\??([^\?]*)$/);
        return match && match[3];
    }

    describe('yandex.state.mapsLinkBuilder', function () {
        var dataStore;
        var map;

        before(function () {
            map = new Map('map', {
                center: [55.755768, 37.617671],
                zoom: 13,
                type: null
            });
        });

        after(function () {
            map.destroy();
        });

        beforeEach(function () {
            dataStore = new YandexState(map);
        });

        it('Должен сгенерировать ссылку без параметров', function () {
            var link = linkBuilder.build(dataStore.getAll());
            expect(test(link, [
                ORIGIN_PARAM,
                FROM_PARAM
            ])).to.be(true);
        });

        it('Должен сгенерировать ссылку с одним параметром (уровень масштабирования)', function () {
            dataStore.set('map', {'zoom': 14});
            var link = linkBuilder.build(dataStore.getAll());
            expect(test(link,[
                ['z', '14'],
                ORIGIN_PARAM,
                FROM_PARAM
            ])).to.be(true);
        });

        it('Должен сгенерировать ссылку с несколькими параметрами', function () {
            dataStore.set('map', {
                zoom: 14,
                center: [11, 11]
            });

            var link = linkBuilder.build(dataStore.getAll());
            expect(test(link, [
                ['z', '14'],
                ['ll', '11,11'],
                ORIGIN_PARAM,
                FROM_PARAM
            ])).to.be(true);
        });
    });

    function test (link, params) {
        var linkParams = getPath(link);
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
