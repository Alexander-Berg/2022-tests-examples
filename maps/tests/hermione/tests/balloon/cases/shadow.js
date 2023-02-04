describe('balloon/shadow.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/shadow.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.balloon.closeButton());
    });

    it('Проверим кастомную тень', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});