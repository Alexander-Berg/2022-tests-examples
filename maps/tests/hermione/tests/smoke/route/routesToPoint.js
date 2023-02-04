describe('smoke/route/routesToPoint.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/routesToPoint.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Строится маршрут при клике по карте', function () {
        return this.browser
            .pointerClick(100, 100)
            .pointerClick('ymaps=Как добраться')
            .pointerClick('ymaps=На автомобиле')
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route')
            .verifyNoErrors();
    });
});
