describe('smoke/geoobject/rectangle.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/rectangle.html')

            .waitReady();
    });

    it('Проверяем редактор линии', function () {
        return this.browser
            //Откроем балун на прямоугольнике
            .click(PO.map.map())

            //Подождём окончания автопана
            .pause(1000)

            //Откроем хинт и сделаем скриншот
            .moveToObject('body', 150, 300)
            .pause(300)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_rectangles')
            .verifyNoErrors();
    });

    it('Драгаем прямоугольник', function () {
        return this.browser
            .csDrag([261, 320], [177, 204])
            .pause(300)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_rectangles_after_drag')
            .verifyNoErrors();
    });
});
