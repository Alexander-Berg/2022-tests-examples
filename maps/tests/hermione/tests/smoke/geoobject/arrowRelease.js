describe('smoke/geoobject/arrowRelease.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/arrowRelease.html')
            .waitReady()
    });

    it('Проверяем появление стрелы на карте', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'before_zoom')
            .verifyNoErrors();
    });

    it('Проверяем, что после зума стрела на месте', function () {
        return this.browser
            .waitAndClick(PO.map.controls.zoom.plus())
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'after_zoom')
            .verifyNoErrors();
    });
});
