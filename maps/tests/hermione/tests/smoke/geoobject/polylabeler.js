describe('smoke/geoobject/polylabeler.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/polylabeler.html')
            .waitReady();
    });

    it('Должны быть подписи у полигонов', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });

    it('Должны быть изображения у полигонов', function () {
        return this.browser
            .waitAndClick(PO.map.controls.listbox())
            .waitAndClick(PO.mapControlsListboxItem() + '=Изображение')
            .waitForVisible(PO.map.placemark.placemark())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});
