describe('smoke/balloonAndHint/balloonPanel.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/balloonAndHint/balloonPanel.html')
            .waitReady();
    });

    it('Панель открывается при клике по метке', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
	    .waitForVisible(PO.map.balloonPanel.content())            
	    .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonPanel')
            .verifyNoErrors();
    });

    it('Панель закрывается по клику на метке', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitForVisible(PO.map.balloonPanel.content())
            .pause(500)
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitForInvisible(PO.map.balloonPanel.content())            
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });

    it('Панель закрывается на крестик', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .pause(500)
            .waitAndClick(PO.map.balloonPanel.closeButton())
	    .waitForInvisible(PO.map.balloonPanel.content())            
	    .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });
});
