describe('smoke/geocode/longlat.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/longlat.html')

            // Ждём карту
            .waitReady();
    });

    it('Геокодирование, балун, хинт и маршрут в longlat', function () {
        return this.browser
            // Строим маршрут
            .waitAndClick(PO.map.controls.button())
            .pause(500)
            .pointerClick(124, 174)
            .pause(500)
            .pointerClick(104, 264)

            // Ждём хинт, балун и маршрут
            .waitForVisible(PO.map.balloon.closeButton())
            .waitForVisible(PO.map.hint.text())
            .waitForVisible(PO.map.pane.areas())

            // Делаем скриншот
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'result')
            .verifyNoErrors();
    });
});
