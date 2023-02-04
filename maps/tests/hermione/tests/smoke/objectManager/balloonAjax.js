describe('smoke/objectManager/balloonAjax.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/objectManager/balloonAjax.html')
            .waitReady();
    });
    it('Появляются данные в балуне метки', function () {
        return this.browser
            .pause(1000)
            .waitForVisible(PO.map.placemark.placemark.icon())
            .pointerClick(PO.map.placemark.placemark.icon())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Появляются данные в балуне кластера', function () {
        return this.browser
            .pause(1000)
            .waitForVisible(PO.map.placemark.placemark())
            .pointerClick(PO.map.placemark.placemark())
            .moveToObject(PO.mapId())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonCluster')
            .verifyNoErrors();
    });
});
