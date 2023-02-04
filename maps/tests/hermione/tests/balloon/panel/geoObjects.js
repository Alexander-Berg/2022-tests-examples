describe('balloon/panel/geoObjects.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/panel/geoObjects.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Балун-панель у круга', function () {
        return this.browser
            .pointerClick(351, 247)
            .waitForVisible(PO.map.balloonPanel.closeButton())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'circlePanel')
            .pointerClick(325, 219)
            .waitForVisible(PO.map.balloonPanel.closeButton())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherCirclePanel')
            .verifyNoErrors();
    });

    it('Балун-панель у метки', function () {
        return this.browser
            .pointerClick(178, 229)
            .waitForVisible(PO.map.balloonPanel.closeButton())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarkPanel')
            .verifyNoErrors();
    });

    it('Балун-панель у прямоугольника', function () {
        return this.browser
            .pointerClick(174, 162)
            .waitForVisible(PO.map.balloonPanel.closeButton())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'rectPanel')
            .verifyNoErrors();
    });

    it('Балун-панель у линии', function () {
        return this.browser
            .pointerClick(82, 338)
            .waitForVisible(PO.map.balloonPanel.closeButton())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'linePanel')
            .verifyNoErrors();
    });

    it('Балун-панель у полигона', function () {
        return this.browser
            .pointerClick(274, 371)
            .waitForVisible(PO.map.balloonPanel.closeButton())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'polygonPanel')
            .verifyNoErrors();
    });
});
