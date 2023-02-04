describe('smoke/geoQuery/showVisibleObjects.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoQuery/showVisibleObjects.html')

            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем что на карту попадают только метки в видимой области', function () {
        return this.browser
            //Проверяем что на карте есть все метки
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'map')

            //Метка пропадает если её хвостик заходит за край карты
            .pointerClick(239, 196)
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'map_without_placemark')

            //Метка появляется если её хвостик в границах карты
            .pointerClick(24, 452)
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_placemarks')
            .verifyNoErrors();
    });
});
