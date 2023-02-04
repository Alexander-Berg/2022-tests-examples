describe('smoke/panorama/custom.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/panorama/custom.html')
            .waitReady(PO.panorama());
    });

    it('Панорама', function () {
        return this.browser
            .waitForVisible(PO.panorama.plus())
            .pause(2000)
            .csVerifyScreenshot('body #player', 'player')
            .verifyNoErrors();
    });

    it('Клик по кастомному контролу ведёт на другую панораму', function () {
        return this.browser
            .waitForVisible(PO.panorama.plus())
            .pointerClick(260, 115)
            .pause(2000)
            .csVerifyScreenshot('body #player', 'player2')
            .verifyNoErrors();
    });
});
