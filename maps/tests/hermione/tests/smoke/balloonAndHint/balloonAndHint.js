describe('smoke/balloonAndHint/balloonAndHint.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/balloonAndHint/balloonAndHint.html')
            .waitReady()
            .waitForVisible(PO.map.balloon.content());
    });

    it('Новый хинт закрывает старый', function () {
        return this.browser
            .moveToObject(PO.map.placemark.placemark())
            .waitUntilTextEql(PO.map.hint.text(), 'Хинт метки')
            .csVerifyMapScreenshot(PO.mapId(), 'hint')
            .verifyNoErrors();
    });

    it('Новый балун закрывает старый', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitUntilTextEql(PO.map.balloon.footer(), 'Подвал')
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Хинт и балун не пропадают при зуме', function () {
        return this.browser
            .waitAndClick(PO.map.controls.zoom.minus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.plus())
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAndHint')
            .verifyNoErrors();
    });
});
