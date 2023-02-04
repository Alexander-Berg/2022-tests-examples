describe('smoke/geocode/direct.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/direct.html')

            // Ждём карту
            .waitReady();
    });

    it('Прямое геокодирование', function () {
        return this.browser
            // Открываем балун метки и делаем скриншот
            .waitForVisible(PO.map.placemark.placemark())
            .pointerClick(PO.map.placemark.placemark())
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'placemark')

            // Проверяем лог
            .csVerifyScreenshot(PO.pageLog(), 'log')
            .verifyNoErrors();

    });
});
