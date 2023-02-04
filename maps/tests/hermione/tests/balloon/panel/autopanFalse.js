describe('balloon/panel/autopanFalse.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/autopanFalse.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.balloonPanel.closeButton());
    });

    it('Открытие панели без автопана, переход панель->балун без автопана', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'panel')
            .pointerClick('ymaps=panel/balloon', 10, 10)
            .pause(100)
            .pointerClick('ymaps=panel/balloon', 10, 10)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherPanel')
            .verifyNoErrors();
    });
});