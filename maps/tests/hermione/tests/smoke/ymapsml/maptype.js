describe('smoke/ymapsml/maptype.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/ymapsml/maptype.html')
            // Дожидаемся видимости карты.
            .waitReady();
    });

    it('Проверяем применение mapState', function () {
        return this.browser
            .waitForVisible(PO.pageReady(), 30000)
            .pause(3000)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });
});
