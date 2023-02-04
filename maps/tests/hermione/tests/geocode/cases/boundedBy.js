describe('geocode/boundedBy.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('geocode/cases/boundedBy.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('without boundedBy', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutBoundedBy')
    });

    it('with boundedBy', function () {
        return this.browser
            .waitAndClick(PO.map.controls.button())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'withBoundedBy');
    });
});
