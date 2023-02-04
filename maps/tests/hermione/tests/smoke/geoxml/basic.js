describe('smoke/geoxml/basic.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoxml/basic.html', 'mock')
            .waitReady();
    });

    it('Показать ymapsml с балуном', function () {
        return this.browser
            .pointerClick('.load-ymapsml')
            .waitAndPointerClick(PO.map.placemark.placemark())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'ymapsml')
            .verifyNoErrors();
    });

    it('Показать kml', function () {
        return this.browser
            .pointerClick('.load-kml')
            .waitForVisible(PO.map.placemark(), 20000)
            .pointerClick(PO.map.placemark.placemark())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'kml')
            .verifyNoErrors();
    });

    it('Показать gpx', function () {
        return this.browser
            .pointerClick('.load-gpx')
            .waitAndPointerClick(PO.map.placemark.placemark())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'gpx')
            .verifyNoErrors();
    });
});
