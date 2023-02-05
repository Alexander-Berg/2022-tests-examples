ymaps.modules.define(util.testfile(), [
    'yandex.timeZone',
    'expect'
], function (provide, timeZone) {

    describe('yandex.timeZone', function () {
        var jsonp = util.mocha.mock.jsonp();

        it('Должен вернуть смещение времени в Новосибирске', function () {
            jsonp.mock.stub(/\/services\/coverage\/v2\/\?.*\bl=trf,trfa\b/)
                .completeWith({"status":"success","data":[{"id":"trf","zoomRange":[0,23],"LayerMetaData":[{"geoId":65,"archive":true,"events":true},{"geoId":11316,"archive":true,"events":true}]},{"id":"trfa","LayerMetaData":[{"geoId":65,"offset":25200,"dst":"std"},{"geoId":11316,"offset":25200,"dst":"std"}]}]})
                .play();

            return timeZone.get([54.98186707183544, 83.02082167773432], 11).then(function (result) {
                expect(result.offset).to.be(25200);
                expect(result.dst).not.to.be(undefined);
            });
        });
    });

    provide();
});
