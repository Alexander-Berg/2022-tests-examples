describe('smoke/balloonAndHint/balloonLayout.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/balloonAndHint/balloonLayout.html')
            .waitReady();
    });

    it('Кастомный макет балуна работает и после зума ничего не ломается', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitAndPointerClick(PO.counterButton())
            .waitAndPointerClick(PO.counterButton())
            .waitAndPointerClick(PO.counterButton())
            .waitAndPointerClick(PO.counterButton())
            .waitAndClick(PO.map.controls.zoom.minus())
            .waitAndClick(PO.map.controls.zoom.plus())
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAfterZoom')
            .verifyNoErrors();
    });
});
