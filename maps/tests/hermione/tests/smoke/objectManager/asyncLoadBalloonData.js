describe('smoke/objectManager/asyncLoadBalloonData.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/objectManager/asyncLoadBalloonData.html')
            .waitReady();
    });

    it('У меток есть хинт', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.placemark())
            .moveToObject(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'hint')
            .verifyNoErrors();
    });

    it('У меток есть балун', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitForVisible(PO.map.balloon.content())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('При зуме метки на месте', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.placemark())
            .click(PO.map.controls.zoom.minus())
            .pause(100)
            .click(PO.map.controls.zoom.minus())
            .pause(100)
            .click(PO.map.controls.zoom.plus())
            .pause(100)
            .click(PO.map.controls.zoom.plus())
            .pause(100)
            .click(PO.map.controls.zoom.plus())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks')
            .verifyNoErrors();
    });
});
