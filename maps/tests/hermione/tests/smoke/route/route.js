describe('smoke/route/route.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/route.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Внешний вид старого маршрутизатора с кастомным цветом и данными о маршруте', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .csVerifyScreenshot('body #list', 'log')
            .verifyNoErrors();
    });
});
