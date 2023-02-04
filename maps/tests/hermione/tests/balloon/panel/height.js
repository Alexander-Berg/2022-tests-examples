describe('balloon/panel/height.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/height.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('При изменении высоты карты балун-панель на месте', function () {
        return this.browser
            .pointerClick(20, 20)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'panel')
            .pointerClick(20, 20)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherPanel')
            .verifyNoErrors();
    });

    it('При удалении метки пропадает балун-панель', function () {
        return this.browser
            .pointerClick('ymaps=remove')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutPanel')
            .verifyNoErrors();
    });

    it('При клике по метке пропадает балун-панель', function () {
        return this.browser
            .pointerClick(PO.map.placemark.placemark())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'afterPlacemarkClick')
            .verifyNoErrors();
    });
});
