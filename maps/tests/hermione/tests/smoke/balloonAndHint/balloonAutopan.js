describe('smoke/balloonAndHint/balloonAutopan.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/balloonAndHint/balloonAutopan.html')
            .waitReady();
    });

    it('Проверяем внешний вид балуна с контентом', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .click('body #set-balloon-header')
            .click('body #set-balloon-content')
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Крестик закрывает балун', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitForVisible('.popover-content')
            .csVerifyMapScreenshot(PO.mapId(), 'balloonWithoutContent')
            .waitAndPointerClick('a.close')
            .waitForInvisible('.popover-content')
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });

    it('Балун не закрывается при зуме', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .waitAndClick(PO.map.controls.zoom.minus())
            .waitAndClick(PO.map.controls.zoom.plus())
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAfterZoom')
            .verifyNoErrors();
    });
});
