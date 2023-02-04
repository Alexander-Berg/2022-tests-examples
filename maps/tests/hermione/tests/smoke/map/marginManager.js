describe('smoke/map/marginManager.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/map/marginManager.html')
            .waitReady()
            .waitForVisible(PO.map.balloon.content());
    });

    it('Отцентрируемся без маргинменеджера', function () {
        return this.browser
            .pointerClick('.no-margin')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });

    it('Отцентрируемся с маргинменеджером', function () {
        return this.browser
            .pointerClick('.with-margin')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map2')
            .verifyNoErrors();
    });
});
