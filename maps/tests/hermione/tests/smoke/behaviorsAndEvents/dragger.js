describe('smoke/behaviorsAndEvents/dragger.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/behaviorsAndEvents/dragger.html')
            .waitReady();
    });

    it('Проверяем работу драгера: Метка должна переместиться', function () {
        return this.browser
            .csDrag([23, 535], [362, 347])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'dragger')
            .verifyNoErrors();
    });
});
