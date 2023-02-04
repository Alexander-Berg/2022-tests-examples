describe('smoke/ymapsml/parentstyle.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/ymapsml/parentstyle.html')
            // Дожидаемся видимости карты и меток.
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Наследование стилей', function () {
        return this.browser
            // Открываем балун и делаем скриншот.
            .pointerClick(153, 188)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});
