describe('smoke/geoobject/circle.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/circle.html')
            .waitReady();
    });

    it('Проверяем появление кругов с обводкой на карте', function () {
        return this.browser
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'before_zoom')
            .verifyNoErrors();
    });

    it('Проверяем наличие хинта', function () {
        return this.browser
            .moveToObject(PO.mapId())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'before_zoom_with_hint')
            .verifyNoErrors();
    });

    it('Проверяем, что после зума с открытым балуном круги на месте', function () {
        return this.browser
            .pointerClick(256, 256)
            .pause(500)
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'after_zoom')
            .verifyNoErrors();
    });
});
