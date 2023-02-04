describe('smoke/objectManager/basic.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/objectManager/basic.html')
            .waitReady();
    });

    it('Кластеризация с уменьшенной сеткой и зелёные метки', function () {
        return this.browser
            .waitAndClick(PO.map.controls.zoom.minus())
            .click(PO.map.controls.zoom.minus())
            .pause(200)
            .waitForVisible(PO.map.placemark.placemark())
            .pause(200)
            .click(PO.map.controls.zoom.plus())
            .pause(200)
            .click(PO.map.controls.zoom.plus())
            .pause(200)
            .click(PO.map.controls.zoom.plus())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks')
            .verifyNoErrors();
    });
});
