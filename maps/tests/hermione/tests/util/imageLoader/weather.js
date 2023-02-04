describe('util/imageLoader/weather.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('util/imageLoader/weather.html')
            .waitReady(PO.mapId());
    });

    it('Проверяем появление тайлов и их подгрузку после зума', function () {
        return this.browser
            .pause(1000)
            .csVerifyScreenshot(PO.mapId(), 'tiles')
            .csDrag([340, 340], [40,40])
            .pause(1000)
            .csVerifyScreenshot(PO.mapId(), 'tilesAfterZoom')
            .verifyNoErrors();
    });
});