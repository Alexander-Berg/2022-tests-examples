describe('balloon/shape.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/shape.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.balloon.closeButton());
    });

    it('Проверяем shape балуна', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .pointerClick(PO.map.balloon.closeButton())
            .waitForVisible(PO.map.balloon.closeButton())
            .pointerClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.closeButton())
            .pointerClick(PO.map.placemark.placemark())
            .pause(1000)
            .csCheckText('body #logger', 'balloon.getOverlay():\n[object Object]\nOK\ngetShape():\n[object Object]\nOK\ngetShape().getType():\nRectangle\nOK\nmap.balloon.getOverlay():\n[object Object]\nOK\ngetShape():\n[object Object]\nOK\ngetShape().getType():\nRectangle\nOK\nplacemark.balloon.getOverlay():\n[object Object]\nOK\ngetShape():\n[object Object]\nOK\ngetShape().getType():\nRectangle\nOK')
            .verifyNoErrors();
    });
});