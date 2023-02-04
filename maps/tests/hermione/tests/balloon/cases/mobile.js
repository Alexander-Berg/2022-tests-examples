describe('balloon/mobile.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/mobile.html', {tileMock: 'blueWithParameters'})
            .waitReady(PO.map.balloonPanel.closeButton());
    });

    it('Внешний вид балуна и балуна-панели первой метки', function () {
        return this.browser
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'panel')
            .pointerClick(PO.map.controls.button())
            .pause(500)
            .pointerClick(PO.map.controls.button())
            .waitForVisible(PO.map.balloon.closeButton())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});