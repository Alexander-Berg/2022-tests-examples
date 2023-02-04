describe('smoke/geoobject/iconSprites.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/iconSprites.html')
            .waitReady();
    });

    it('Проверяем кастомные иконки меток', function () {
        return this.browser
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.minus())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_load')
            .verifyNoErrors();
    });

    it('Проверяем открытие балуна', function () {
        return this.browser
            .pointerClick(142, 227)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'panel')
            .verifyNoErrors();
    });
});
