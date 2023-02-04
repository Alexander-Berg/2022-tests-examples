describe('smoke/geoobject/events.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/events.html')

            //Появилась карта.
            .waitReady();
    });

    it('Проверяем события круга', function () {
        return this.browser
            //Проверяем наличие негеодезического круга и хинта
            .moveToObject('body', 263, 278)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'circle_with_hint')

            //Нажимаем на все кнопки
            .moveToObject('body', 0, 0)
            .pause(1000)
            .pointerClick(90, 50)
            .pointerClick(230, 50)
            .pointerClick(380, 50)

            //Проверяем наличие геодезического круга и текст в балуне
            .pause(1000)
            .pointerMoveTo(PO.mapId(), 256, 256)
            .pause(1000)
            .pointerClick(PO.mapId(), 256, 256)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'circle_with_balloon')
            .waitAndClick(PO.map.balloon.closeButton())

            //Проверяем события
            .pause(1000)
            .csVerifyScreenshot(PO.pageLog(), 'log')
            .verifyNoErrors();
    });
});
