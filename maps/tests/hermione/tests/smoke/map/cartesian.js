describe('smoke/map/cartesian.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/modules/area.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проекция cartesian', function () {
        return this.browser
            // Делаем скриншот
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'map');
    });

    it('Проекция cartesian после зума', function () {
        return this.browser
            // Призумливаемся и делаем скриншот
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'another_map');
    });
});
