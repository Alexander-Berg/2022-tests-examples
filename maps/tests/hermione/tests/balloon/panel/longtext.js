describe('balloon/panel/longtext.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/longtext.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark())
            .waitForVisible(PO.map.balloonPanel.closeButton());
    });
    // Не факт что это сработает, если вдруг баг будет, но есть-пить не просит.
    it('Панель не должна сдвигаться при попытке её драгнуть', function () {
        return this.browser
            .csDrag([400, 400], [200,200])
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'panel')
            .verifyNoErrors();
    });
});