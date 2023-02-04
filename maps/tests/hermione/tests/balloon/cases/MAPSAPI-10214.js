describe('balloon/MAPSAPI-10214.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/MAPSAPI-10214.html', {tileMock: 'blueWithParameters'})
            .waitReady(PO.map.balloon.closeButton());
    });

    it('Балун не перемещается при драге карты', function () {
        return this.browser
            .pause(3000)
            .csDrag([100, 100], [200, 200])
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAfterDrag')
            .verifyNoErrors();
    });
});