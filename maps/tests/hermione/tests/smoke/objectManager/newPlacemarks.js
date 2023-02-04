describe('smoke/objectManager/newPlacemarks.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/objectManager/newPlacemarks.html')
            .waitReady();
    });
    //TODO: драг не работает
    it.skip('При зуме и драге ничего не ломается', function () {
        return this.browser
            .pause(2000)
            .csDrag([40, 40], [40,0])
            .pause(1000)
            .pointerDblClick(PO.map.pane.events())
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'geoobjects')
            .verifyNoErrors();
    });
});
