describe('smoke/geoobject/collection.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/collection.html')
            //Появилась карта и метки на ней
            .waitReady()
            .waitForVisible(PO.map.placemark.svgIcon());
    });

    it('Проверяем коллекции меток', function () {
        return this.browser
            //Кликаем по каждой метке по одному разу
            .pointerClick(267, 241)
            .pause(200)
            .pointerClick(267, 188)
            .pause(200)
            .pointerClick(231, 188)
            .pause(200)
            .pointerClick(231, 241)

            //Проверяем наличие меток на карте и реакцию на клик в логе
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .csVerifyScreenshot(PO.pageLog(), 'log')
            .verifyNoErrors();

    });
});
