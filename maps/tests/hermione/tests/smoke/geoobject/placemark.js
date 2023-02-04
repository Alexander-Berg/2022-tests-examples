describe('smoke/geoobject/placemark.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/placemark.html')

            //Появилась карта и метки на ней.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем разные метки', function () {
        return this.browser
            //Проверяем наличие меток на карте и что у одной из них открыт балун
            .pointerClick(382, 214)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarks')
            //Проверяем что по клику "Открыть в Я.Картах" открывается моя карта на БЯК
            .pointerClick(100,500)
            .verifyNoErrors()
            .getTabIds().then(function(e){
                 this.switchTab(e[1])
            })
            .waitForVisible('.user-maps-panel-view__header', 20000)
            .getText('.user-maps-panel-view__header').then(function(text){
                text.should.equal('Офисы яндекса')
            });
    });
});
