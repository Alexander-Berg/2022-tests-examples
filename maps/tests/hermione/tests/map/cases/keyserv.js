describe('map/keyserv.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('map/cases/keyserv.html', false)
            .waitReady();
    });
    it('Апи загрузилось на странице с параметром key', function () {
        return this.browser
            .pause(1000)
            .csVerifyScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });
});
