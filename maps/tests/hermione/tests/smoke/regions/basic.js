describe('smoke/regions/basic.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/regions/basic.html')
            .waitReady()
            .waitForVisible(PO.map.pane.areas());
    });

    it('Появились регионы и у них есть хинт', function () {
        return this.browser
            .moveToObject(PO.mapId())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'hint')
            .verifyNoErrors();
    });
});
