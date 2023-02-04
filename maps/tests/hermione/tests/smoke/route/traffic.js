describe('smoke/route/traffic.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/traffic.html')
            .waitReady()
            .waitForVisible(PO.routePoints())
    });

    it('Пробки не меняют маршрут ОТ, но меняют авто', function () {
        return this.browser
            .pointerClick('ymaps=Учитывать пробки')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'route1')
            .pointerClick('ymaps=ПереключитьАвто')
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route2')
            .verifyNoErrors();
    });
});
