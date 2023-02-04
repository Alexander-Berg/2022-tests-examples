describe('smoke/geoobject/contextmenu.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/contextmenu.html')

            //Появилась карта и метки на ней.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    hermione.only.in('chrome');
    it('Проверяем изменение properties метки', function () {
        return this.browser
            //Кликаем по метке правой кнопкой, заполняем поля и сохраняем.
            .pointerRightClick(376, 237)
            .waitForVisible('body #icon_text')
            .setValue('body #icon_text', 'icon_text')
            .setValue('body #hint_text', 'hint_text')
            .setValue('body #balloon_text', 'balloon_text')
            .click('body #submit')
            .moveToObject(PO.map.placemark.placemark())

            //Проверяем текст метки и хинт.
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_hint')

            //Проверяем текст в балуне
            .pointerClick(376, 237)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_balloon')

            //Балун должен закрыться.
            .pointerClick(PO.map.balloon.closeButton())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map_without_balloon')
            .verifyNoErrors();
    });
});
