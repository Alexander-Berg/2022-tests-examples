describe('smoke/route/viewAccess.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/viewAccess.html')
            .waitReady()
            .waitForVisible(PO.routePoints.placemark());
    });

    it('Внешний вид альтернативного маршрута с балуном и кастомной закраской', function () {
        return this.browser
            .pause(2000)
            .pointerClick(PO.routePoints.pinA())
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'routeWithBalloon')
            .verifyNoErrors();
    });
});
