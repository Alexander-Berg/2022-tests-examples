describe('smoke/panorama/basics.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/panorama/basics.html')
            .waitReady(PO.panorama());
    });

    it('Проверяем внешний вид панорам', function () {
        return this.browser
            .waitForVisible(PO.panorama.plus())
            .pause(2000)
            .csVerifyScreenshot('body #player1', 'player1')
            .csVerifyScreenshot('body #player2', 'player2')
            .verifyNoErrors();
    });
    //TODO: драг не работает
    it.skip('Панорама драгается', function () {
        return this.browser
            .waitForVisible(PO.panorama.plus())
            .csDrag([100, 100], [150, 150])
            .csDrag([500, 500], [550, 500])
            .pause(2000)
            .csVerifyScreenshot('body #player1', 'player1_after_drag')
            .csVerifyScreenshot('body #player2', 'player2_after_drag')
            .verifyNoErrors();
    });
});
