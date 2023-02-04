describe('geocode/coordOrderGlobal.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('geocode/cases/coordOrderGlobal.html', {tileMock: "withParameters"})
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });
    it('Поиск всегда должен быть в Москве. Видимых изменений при нажатии на кнопку быть не должно', function () {
        return this.browser
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .pointerClick(128, 21)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'map2')
            .pause(500)
            .pointerClick(128, 21)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map');
    });
});
