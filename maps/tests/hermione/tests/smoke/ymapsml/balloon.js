describe('smoke/ymapsml/balloon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/ymapsml/balloon.html')
            //дожидаемся видимости карты и метки
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('кастомная метка и балун с разметкой', function () {
        return this.browser
            //открываем балун и снимаем скриншот
            .pointerClick(157, 378)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});
