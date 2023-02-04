describe('balloon/panelMaxHeightRatio.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/panelMaxHeightRatio.html', {tileMock: 'blueWithParameters'})
            .waitReady(PO.map.placemark.placemark());
    });

    it('maxHeightRatio 0', function () {
        return this.browser
            .pointerClick(235, 228)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'maxHeightRatio0')
            .verifyNoErrors();
    });

    it('maxHeightRatio 1', function () {
        return this.browser
            .pointerClick(304, 267)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'maxHeightRatio1')
            .verifyNoErrors();
    });
});