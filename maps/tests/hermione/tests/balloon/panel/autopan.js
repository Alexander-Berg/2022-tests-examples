describe('balloon/panel/autopan.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/autopan.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.balloonPanel.closeButton());
    });
    it('Проверим внешний вид балунов-панелей', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'panel')
            .pointerClick(307, 33)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherPanel')
            .verifyNoErrors();
    });
});