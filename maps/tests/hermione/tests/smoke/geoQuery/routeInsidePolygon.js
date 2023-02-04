describe('smoke/geoQuery/routeInsidePolygon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoQuery/routeInsidePolygon.html')

            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем маршрут внутри границ москвы', function () {
        return this.browser
            //Проверяем что на карте появился маршрут
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });
});
