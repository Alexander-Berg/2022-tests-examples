describe('geocode/coordOrder.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('geocode/cases/coordOrder.html', {tileMock: "withParameters"})
            .waitReady(PO.map.placemark.placemark());
    });

    it('searchCoordOrder: longlat', function () {
        return this.browser
            .waitAndClick(PO.map.controls.button())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'longlat');
    });
    it('searchCoordOrder: latlong', function () {
        return this.browser
            .waitAndClick(PO.map.controls.button())
            .pause(500)
            .waitAndClick(PO.map.controls.button())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'latlong');
    });
});
