describe('balloon/MAPSAPI-8858(1).html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/MAPSAPI-8858(1).html')
            .waitReady()
            .waitForVisible(PO.map.balloon.closeButton());
    });

    it('Балун не появляется после закрытия при драге и зуме', function () {
        return this.browser
            .pointerClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.closeButton())
            .csDrag([100,100], [200,200])
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(1000)
            .pointerClick(PO.map.controls.zoom.minus())
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutBalloon')
            .verifyNoErrors();
    });
});