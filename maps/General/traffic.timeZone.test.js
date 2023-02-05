ymaps.modules.define(util.testfile(), [
    'traffic.timeZone'
], function (provide, timeZone) {
    describe('traffic.TimeZone', function () {
        var COVERAGE_MATCHER = /\/services\/coverage\/v2\/\?.*\bl=trf,trfa\b/;
        var COVERAGE_JSON = {"status":"success","data":[{"id":"trf","zoomRange":[0,23],"LayerMetaData":[{"geoId":213,"archive":true,"events":true}]}, {"id":"trfa","zoomRange":[0,23],"LayerMetaData":[{"geoId":213,"offset":10800,"dst":"std"}]}]};

        var jsonp = util.mocha.mock.jsonp();

        it('Должен запросить и получить таймзону', function () {
            jsonp.mock.stub(COVERAGE_MATCHER).completeWith(COVERAGE_JSON).play();

            return timeZone.get([55.770569, 37.648452], 10).then(function (data) {
                expect(data.offset).to.be(10800);
                expect(data.dst).to.be('std');
            });
        });
    });

    provide({});
});
