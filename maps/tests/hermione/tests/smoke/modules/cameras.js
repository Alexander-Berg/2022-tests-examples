describe('smoke/modules/cameras.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/modules/cameras.html')
            .waitReady();
    });

    it('Проверим работу тепловой карты после зума', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });
});
