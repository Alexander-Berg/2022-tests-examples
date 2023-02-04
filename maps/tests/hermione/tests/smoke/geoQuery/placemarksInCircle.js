describe('smoke/geoQuery/placemarksInCircle.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoQuery/placemarksInCircle.html')

            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем searchInside', function () {
        return this.browser
            //Проверяем что на карте появился круг и метки
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'map')

            //Проверяем что метка поменяла цвет после того как попала в круг
            .pointerClick(251, 168)
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'red_placemark')

            //Проверяем что метка вернула прежний цвет, а другая метка поменяла после того как попала в круг
            .pointerClick(187, 331)
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'another_red_placemark')
            .verifyNoErrors();
    });
});
