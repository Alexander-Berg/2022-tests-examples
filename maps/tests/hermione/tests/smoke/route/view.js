describe('smoke/route/view.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/view.html')
            .waitReady()
            .waitForVisible(PO.routePoints.placemark(), 20000);
    });

    it('Внешний вид маршрута с кастомными точками', function () {
        return this.browser
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route')
            .verifyNoErrors();
    });

    it('Удаляется и возвращается промежуточная точка', function () {
        return this.browser
            .pointerClick('ymaps=УдалитьПромежуточныеТочки')
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'routeWithoutVia')
            .pointerClick('ymaps=УдалитьПромежуточныеТочки')
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route')
            .verifyNoErrors();
    });

    it('Меняется тип маршрута', function () {
        return this.browser
            .pointerClick('ymaps=ТипМаршрута')
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route2')
            .verifyNoErrors();
    });
});
