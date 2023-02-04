describe('balloon/panel/copyrights.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/copyrights.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Задание контента балуна-панели', function () {
        return this.browser
            .pointerClick(PO.map.placemark.placemark())
            .waitForVisible(PO.map.balloonPanel.closeButton())
            .csVerifyMapScreenshot(PO.mapId(), 'panel')
            .pointerClick('ymaps=content')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherPanel')
            .verifyNoErrors();
    });
});