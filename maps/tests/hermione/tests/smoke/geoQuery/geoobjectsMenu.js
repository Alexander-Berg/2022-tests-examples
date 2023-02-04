describe('smoke/geoQuery/geoobjectsMenu.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoQuery/geoobjectsMenu.html')

            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем search', function () {
        return this.browser
            //Проверяем что на карте есть все геообъекты
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'map')

            //Проверяем что пропали метки
            .waitAndClick('body #point')
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'map_without_points')

            //Проверяем что пропали желтые геообъекты
            .waitAndClick('body #yellow')
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'map_without_yellow')

            //Проверяем появились геообъекты и порядок геообъектов стал другим
            .waitAndClick('body #yellow')
            .waitAndClick('body #point')
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'another_map')
            .verifyNoErrors();
    });
});
