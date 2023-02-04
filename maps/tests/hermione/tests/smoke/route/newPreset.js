describe('smoke/route/newPreset.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/newPreset.html')
            .waitReady()
            .waitForVisible(PO.routePoints.pinA());
    });

    it('Можно получить данные маршрута авто', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'auto')
            .verifyNoErrors();
    });

    it('Можно получить данные маршрута ОТ', function () {
        return this.browser
            .pointerClick('ymaps=Как добраться')
            .pointerClick('ymaps=Общественным транспортом')
            .pause(3000)
            .csVerifyMapScreenshot(PO.mapId(), 'mass')
            .verifyNoErrors();
    });

    it('Можно получить данные маршрута пешки', function () {
        return this.browser
            .pointerClick('ymaps=Как добраться')
            .pointerClick('ymaps=Пешком')
            .pause(3000)
            .csVerifyMapScreenshot(PO.mapId(), 'pedestrian')
            .verifyNoErrors();
    });
});
