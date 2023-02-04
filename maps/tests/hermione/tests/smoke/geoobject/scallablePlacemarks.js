describe('smoke/geoobject/scallablePlacemarks.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/scallablePlacemarks.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Метки разного размера и зависят от зума', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'placemark')
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarkAfterZoom')
            .verifyNoErrors();
    });
});
