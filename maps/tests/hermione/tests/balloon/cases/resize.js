describe('balloon/resize.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/resize.html', {tileMock: 'blueWithParameters'})
            .waitReady();
    });

    it('Балун с кнопкой на разных экранах', function () {
        return this.browser
            .pointerClick(230, 229)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .pointerClick('ymaps=S')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'panel')
            .verifyNoErrors();
    });

    it('Балун выходящий за край карты на разных экранах', function () {
        return this.browser
            .pointerClick(305, 269)
            .pause(1000)
            .csVerifyMapScreenshot('body', 'balloon2')
            .pointerClick('ymaps=S')
            .pause(1000)
            .csVerifyMapScreenshot('body', 'panel2')
            .verifyNoErrors();
    });
});