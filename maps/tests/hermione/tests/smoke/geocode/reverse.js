describe('smoke/geocode/reverse.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/reverse.html')

            //Ждём карту и метки
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем обратное геокодирование с kind: metro', function () {
        return this.browser
            //Открываем балун метки и делаем скриншот
            .pointerClick(PO.map.placemark.placemark())
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'result')
            .verifyNoErrors();

    });
});
