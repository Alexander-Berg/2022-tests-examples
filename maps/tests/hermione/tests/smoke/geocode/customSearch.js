describe('smoke/geocode/customSearch.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/customSearch.html')

            // Ждём карту
            .waitReady();
    });

    it('Поиск по своим объектам', function () {
        return this.browser
            // Вводим гараж, нажимаем найти
            .waitForVisible(PO.map.controls.search.large.button()).then(function(){})
            .setValue(PO.map.controls.search.large.input(), "гараж")
            .pointerClick(PO.map.controls.search.large.button())

            // Делаем скриншот
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'placemark')

            // Вводим трактир, нажимаем найти
            .pointerClick(PO.map.controls.search.large.clear())
            .setValue(PO.map.controls.search.large.input(), "Трактир")
            .pointerClick(PO.map.controls.search.large.button())

            // Открываем балун метки и делаем скриншот
            .pointerClick(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'another_placemark')
            .verifyNoErrors();
    });
});
