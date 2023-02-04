describe('smoke/route/masstransit.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/masstransit.html')
            .waitReady()
            .waitForVisible(PO.routePoints.placemark());
    });

    it('Внешний вид маршрута с указанием времени и расстояния ходьбы', function () {
        return this.browser
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route1')
            .verifyNoErrors();
    });

    it('Расстояние ходьбы выключается', function () {
        return this.browser
            .waitAndClick(PO.map.controls.button())
            .waitForVisible(PO.routePoints.pedestrianPin())
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'route2')
            .verifyNoErrors();
    });
});
