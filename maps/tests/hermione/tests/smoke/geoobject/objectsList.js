describe('smoke/geoobject/objectsList.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/objectsList.html')

            //Появилась карта и метки на ней.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем реакцию на клики вне карты', function () {
        return this.browser
            //Проверяем наличие меток на карте и что у одной из них открыт балун
            .click('body #АркаДружбынародов')
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks')

            //Проверяем, что метки пропали
            .click('body #Покушайки')
            .click('body #Оригинальныемузейчики')
            .click('body #Красивости')
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks_after_removing')
            .verifyNoErrors();
    });
});
