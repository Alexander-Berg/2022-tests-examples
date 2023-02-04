describe('smoke/objectManager/remoteObjectManager.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/objectManager/remoteObjectManager.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('После зума и фулскрина данные на месте', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.map.pane.events(), 'geoobjects1')
            .pointerClick(PO.map.controls.fullscreen())
            .pause(1000)
            .csVerifyMapScreenshot(PO.map.pane.events(), 'geoobjects2')
            .pointerDblClick(639, 635)
            .pause(2000)
            .csVerifyMapScreenshot(PO.map.pane.events(), 'geoobjects3')
            .csDrag([362, 347],[42, 127], 500)
            .pause(3000)
            .csVerifyScreenshot(PO.map.pane.events(), 'geoobjects4')
            .verifyNoErrors();
    });
});
