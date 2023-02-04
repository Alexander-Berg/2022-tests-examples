describe('smoke/modules/load.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/modules/load.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('На карте есть метка и у неё есть балун', function () {
        return this.browser
            .pointerClick(PO.map.placemark.placemark())
            .waitForVisible(PO.map.balloon.content())
            .getText(PO.map.balloon.content()).then(function(text){
                text.should.equal('Нант - шестой по величине город Франции');
            })
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });
});
