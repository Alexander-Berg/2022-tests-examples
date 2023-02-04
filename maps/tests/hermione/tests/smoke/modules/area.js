describe('smoke/modules/area.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/modules/area.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверим работу модуля рассчёт площадей', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });
});
