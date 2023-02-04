describe('smoke/route/dataAccess.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/dataAccess.html')
            .waitReady()
            .waitForVisible(PO.routePoints.placemark());
    });

    it('Можно получить данные маршрута авто', function () {
        return this.browser
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'auto')
            .csVerifyScreenshot('body #viewContainer', 'autoLog')
            .verifyNoErrors();
    });

    it('Можно получить данные маршрута ОТ', function () {
        return this.browser
            .pointerClick('ymaps=Как добраться')
            .pointerClick('ymaps=Общественным транспортом')
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'mass')
            .csVerifyScreenshot('body #viewContainer', 'massLog')
            .verifyNoErrors();
    });

    it('Можно получить данные маршрута пешки', function () {
        return this.browser
            .pointerClick('ymaps=Как добраться')
            .pointerClick('ymaps=Пешком')
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'pedestrian')
            .csVerifyScreenshot('body #viewContainer', 'pedestrianLog')
            .verifyNoErrors();
    });
});
