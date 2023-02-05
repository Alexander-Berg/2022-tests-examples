ymaps.modules.define(util.testfile(), [
    'Map',
    'traffic.provider.Archive',
    'expect'
], function (provide, Map, ArchiveProvider, expect) {
    var map;
    var provider;

    var COVERAGE_MATCHER = /\/services\/coverage\/v2\/\?.*\bl=trf,trfa\b/;
    var COVERAGE_JSON = {"status":"success","data":[{"id":"trf","zoomRange":[0,23],"LayerMetaData":[{"geoId":213,"archive":true,"events":true}]}, {"id":"trfa","zoomRange":[0,23],"LayerMetaData":[{"geoId":213,"offset":10800,"dst":"std"}]}]};

    var TRAFFIC_LIGHT_MATCHER = /\/services\/traffic\/v1\/traffic-light/;
    var TRAFFIC_LIGHT_JSON = {"data":{"type":"FeatureCollection","properties":{"HotspotSearchMetaData":{"HotspotSearchRequest":{"layer":"trjl","id":[213],"lang":"ru_RU"},"HotspotSearchResponse":{"layer":"trjl","found":1}}},"features":[{"type":"Feature","properties":{"HotspotMetaData":{"id":213},"JamsMetaData":{"timestamp":78300,"isotime":"19700101T214500","localtime":"00:45:00","length":0,"icon":"green","level":0,"tend":0,"region":213},"name":"Москва"},"geometry":{"type":"Point","coordinates":[37.622504,55.753215],"name":"Москва"}}]}};

    var TRFA_MATCHER = function (tm, req) {
        return req.startsWith(util.env(ymaps).hosts.trafficArchive + '/tiles') &&
            req.query.tm === String(tm) &&
            req.query.l === 'trf';
    };

    var CENTER = [54, 36];

    describe('traffic.ArchiveProvider', function () {
        this.retries(0);

        var imageLoader = util.mocha.mock.imageLoader();

        var jsonp = util.mocha.mock.jsonp();
        var coverageStub;

        beforeEach(function () {
            CENTER[0] += 0.000001;
            map = new Map('map', {
                center: CENTER,
                zoom: 10,
                behaviors: ['default', 'scrollZoom'],
                type: null
            });

            coverageStub = jsonp.mock.stub(COVERAGE_MATCHER).completeWith(COVERAGE_JSON);
            jsonp.mock.stub(TRAFFIC_LIGHT_MATCHER).completeWith(TRAFFIC_LIGHT_JSON).play();

            provider = new ArchiveProvider({showCurrentTimeFirst: false});
        });

        afterEach(function () {
            provider.setMap(null);
            map.destroy();
        });

        it('Должен инициироваться после выставления timeZone=0', function () {
            provider.setMap(map);

            var trfaStub = imageLoader.mock.stub(TRFA_MATCHER.bind(null, '28800'), 'TRFA_28800')
                .completeWith(util.nullGif)
                .play();

            provider.state.set({timeZone: 0, dst: 'dst', dayOfWeek: 'mon', hours: 0, minutes: 0});
            expect(provider.state.get('isInited')).to.be(true);

            return coverageStub.once()
                .then(util.poll$(function () { return trfaStub.processed.length === 16; }));
        });

        it('Должен выставить последнее переданное в setTime значение', function () {
            provider.setMap(map);

            var trfaStub;

            return coverageStub.once()
                .then(function () {
                    trfaStub = imageLoader.mock.stub(TRFA_MATCHER.bind(null, '33300'), 'TRFA_33300')
                        .completeWith(util.nullGif)
                        .play();

                    return new ymaps.vow.Promise.all([
                        new ymaps.vow.Promise(function (resolve) { provider.setTime({hours: 14, minutes: 20}, resolve); }),
                        new ymaps.vow.Promise(function (resolve) { provider.setTime({hours: 12, minutes: 15}, resolve); })
                    ]);
                })
                .then(function () {
                    // Sometimes setTime callback is called synchroniously, don't rely on it.
                    var time = provider.getTime();
                    expect(time.hours).to.be(12);
                    expect(time.minutes).to.be(15);
                })
                .then(util.poll$(function () { return trfaStub.processed.length === 16; }))
        });
    });

    provide({});
});
