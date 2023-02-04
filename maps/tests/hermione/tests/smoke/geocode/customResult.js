describe('smoke/geocode/customResults.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/customResults.html')

            // Ждём карту
            .waitReady();
    });

    it('Кастомный поиск', function () {
        return this.browser
            // Вводим москва и проверяем саджест
            .waitForVisible(PO.map.controls.search.large.input())
            .setValue(PO.map.controls.search.large.input(), "Москва")
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'suggest',{
                ignoreElements: [PO.map.controls.search.large.input()]
            })

            // Нажимаем найти и проверяем появление метки на карте с хинтом
            .waitAndClick(PO.map.controls.search.large.button())
            .waitForVisible(PO.map.placemark.placemark())
            .moveToObject(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'placemark')

            // Выбираем метку и проверяем балун метки
            .pointerClick(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')

            // Удаляем результаты поиска и закрываем балун, метка остаётся
            .waitAndClick(PO.map.controls.search.large.clear())
            .waitAndClick(PO.map.balloon.closeButton())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'red_placemark')
            .verifyNoErrors();
    });
});
