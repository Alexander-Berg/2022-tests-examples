describe('smoke/route/customBalloon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/customBalloon.html')
            .waitReady()
            .waitForVisible(PO.routePoints.placemark());
    });

    it('Внешний вид кастомного балуна, балун закрывается по клику', function () {
        return this.browser
            .pointerClick(PO.routePoints.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .pointerClick('a.close')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutBalloon')
            .verifyNoErrors();
    });
});
