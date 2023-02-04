describe('smoke/geocode/multigeocode.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/multigeocode.html')

            // Ждём карту
            .waitReady()

            // Ждём балун
            .waitForVisible(PO.map.balloon.closeButton());
    });

    it('Проверяем множественное геокодирование', function () {
        return this.browser
            // Делаем скриншот
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'result')
            .verifyNoErrors();
    });
});
