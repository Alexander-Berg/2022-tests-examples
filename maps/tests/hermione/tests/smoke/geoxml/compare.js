describe('smoke/geoxml/compare.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoxml/compare.html')
            .waitReady();
    });

    it('Скриншот после открытия балуна', function () {
        return this.browser
            .waitAndClick(PO.map.balloon.content())
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});
