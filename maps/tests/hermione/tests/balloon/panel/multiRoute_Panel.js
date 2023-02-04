describe('balloon/panel/multiRoute&Panel.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/multiRoute&Panel.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.routePoints.placemark());
    });

    it('Открытие балуна на маршруте закрывает балун-панель', function () {
        return this.browser
            .pause(1000)
            .pointerClick(PO.routePoints.pinB())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});