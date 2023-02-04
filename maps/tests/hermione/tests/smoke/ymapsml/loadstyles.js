describe('smoke/ymapsml/loadstyles.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/ymapsml/loadstyles.html')
            // Дожидаемся видимости карты и метки.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Подгрузка стилей из отдельного файла', function () {
        return this.browser
            // Делаем скриншот.
            .csVerifyMapScreenshot(PO.mapId(), 'geoobjects')
            .verifyNoErrors();
    });
});
