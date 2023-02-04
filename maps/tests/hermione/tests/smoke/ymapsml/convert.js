describe('smoke/ymapsml/convert.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/ymapsml/convert.html')
            // Дожидаемся видимости карты и меток.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Преобразование в YMapsML из GeoRSS', function () {
        return this.browser
            // Открываем балун и делаем скриншот.
            .pointerClick(135, 210)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});
