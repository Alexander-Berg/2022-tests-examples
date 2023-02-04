describe('smoke/route/create.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/create.html')
            .waitReady()
            .waitForVisible(PO.routePoints.placemark());
    });

    it('Пробки учитываются в обычном маршруте', function () {
        return this.browser
            .pointerClick('ymaps=Учитывать пробки')
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'route')
            .verifyNoErrors();
    });

    it('Внешний вид балуна маршрута без кнопки', function () {
        return this.browser
            .pointerClick(PO.routePoints.placemark())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Маршруту можно добавить и убрать транзитную точку', function () {
        return this.browser
            .pointerClick('ymaps=Добавить транзитную точку')
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'routeWithVia')
            .pointerClick('ymaps=Добавить транзитную точку')
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'routeWithoutVia')
            .verifyNoErrors();
    });
});
