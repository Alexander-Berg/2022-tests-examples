describe('balloon/preventAutoPan.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/preventAutoPan.html', {tileMock: 'blueWithParameters'})
            .waitReady();
    });

    it('Автопан не работает', function () {
        return this.browser
            .pointerClick('ymaps=false')
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});