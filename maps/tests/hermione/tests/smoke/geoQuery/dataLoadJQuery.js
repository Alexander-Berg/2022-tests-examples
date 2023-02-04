describe('smoke/geoQuery/dataLoadJQuery.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoQuery/dataLoadJQuery.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем addToMap и applyBoundsToMap', function () {
        return this.browser
            //Проверяем что на карте есть все результаты и правильно выставлены опции
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks')

            //Проверяем что открылся балун у результатов
            .pointerClick(70, 257)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks_with_balloon')
            .verifyNoErrors();
    });
});
