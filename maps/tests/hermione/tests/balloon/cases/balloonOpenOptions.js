describe('balloon/balloonOpenOptions.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/balloonOpenOptions.html', {tileMock: 'blueWithParameters'})
            .waitReady();
    });

    it('Проверяем работу кейса', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .waitAndClick(PO.map.controls.button())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'reopenedBalloon')
            .verifyNoErrors();
    });
});