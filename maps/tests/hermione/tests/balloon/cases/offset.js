describe('balloon/offset.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/offset.html', {tileMock: 'blueWithParameters'})
            .waitReady(PO.map.balloon.content());
    });

    it('Проверяем оффсет балуна', function () {
        return this.browser
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Проверяем оффсет балуна после автопана', function () {
        return this.browser
            .pointerClick('ymaps=[61, 32]')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAutoPan')
            .verifyNoErrors();
    });

    it('Проверяем оффсет балуна после автопана с autoPanMargin', function () {
        return this.browser
            .pointerClick('ymaps=autoPan')
            .pause(3000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAutoPanMargin')
            .verifyNoErrors();
    });
});