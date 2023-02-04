describe('smoke/balloonAndHint/storage.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/balloonAndHint/storage.html')
            .waitReady();
    });

    it('При открытии кейса открыт балун', function () {
        return this.browser
            .waitForVisible(PO.map.balloon.closeButton())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Балун закрывается по клику и хинт открывается при наведении', function () {
        return this.browser
            .waitAndClick(PO.map.balloon.closeButton())
            .waitForVisible(PO.map.placemark.placemark())
            .moveToObject(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'hint')
            .verifyNoErrors();
    });
});
