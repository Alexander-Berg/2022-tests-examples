describe('balloon/MAPSAPI-9111.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/MAPSAPI-9111.html', {tileMock: 'blueWithParameters'})
            .waitReady(PO.map.dotIcon());
    });

    it('Не улетаем в Гренландию', function () {
        return this.browser
            .pointerDblClick(PO.map.dotIcon())
            .pause(1000)
            .pointerClick(331, 220)
            .csVerifyMapScreenshot(PO.mapId(), 'afterDblClick')
            .verifyNoErrors();
    });
});