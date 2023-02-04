describe('smoke/geocode/eventReverse.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/eventReverse.html')

            // Ждём карту
            .waitReady();
    });

    it.skip('Геокодирование по клику на карту', function () {
        return this.browser
            // Кликаем по карте и делаем скриншот
            .leftClick(PO.mapId(), 241, 350)
            .waitForVisible('ymaps=улица Красного Маяка, 15к1с2')
            .csVerifyMapScreenshot(PO.mapId(), 'result')

            // Кликаем по карте и делаем скриншот
            .moveToObject(PO.mapId(), 310, 77)
            .leftClick(PO.mapId(), 310, 77)
            .waitForVisible('ymaps=Учинское водохранилище')
            .csVerifyMapScreenshot(PO.mapId(), 'another_result')
            .verifyNoErrors();

    });
});
