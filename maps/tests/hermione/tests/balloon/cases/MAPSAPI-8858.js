describe('balloon/MAPSAPI-8858.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/MAPSAPI-8858.html')
            .waitReady()
            .waitForVisible(PO.map.balloon.closeButton());
    });

    it('Балун не появляется после закрытия при драге и зуме', function () {
        return this.browser
            .pause(2000)
            .getText(PO.map.balloon.content()).then(function(text){
                text.should.equal('Одинокий балун: single balloon');
            })
            .pointerClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(1000)
            .csDrag([100,100], [200,200])
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(1000)
            .pointerClick(PO.map.controls.zoom.minus())
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(1500)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutBalloon')
            .verifyNoErrors();
    });
});