describe('multiRouter/MAPSAPI-13360.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('multiRouter/cases/MAPSAPI-13360.html', false)
            .waitReady()
            .waitForVisible(PO.routePoints.pinA());
    });

    it('В ОТ при наличии только пешеходных участков, балун должен выглядеть пешеходным', function () {
        return this.browser
            .pause(500)
            .waitAndClick(PO.routePoints.pinA())
            .pause(500)
            .csVerifyScreenshot(PO.mapId(), 'balloon');
    });
});
