describe('balloon/images.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/images.html')
            .waitReady();
    });

    it('Контент вмещается в балун метки', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.placemark.redPlacemark())
            .pointerClick(PO.map.placemark.placemark.redPlacemark())
            .pause(1000)
            .csVerifyScreenshot(PO.mapId(), 'smallImage')
            .verifyNoErrors();
    });

    it('Контент не вмещается в балун метки', function () {
        return this.browser
            .pointerClick(PO.map.controls.button())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'largeImage')
            .verifyNoErrors();
    });

    it('Контент не вмещается в балун кластера', function () {
        return this.browser
            .pause(1000)
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'largeImageInClusterBalloon')
            .verifyNoErrors();
    });
});