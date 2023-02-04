describe('smoke/balloonAndHint/hintLayout.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/balloonAndHint/hintLayout.html')
            .waitReady();
    });

    it('Хинт открывается при наведении, перемещается по объекту и пропадает', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.svgIcon())
            .moveToObject(PO.map.placemark.svgIcon())
            .waitForVisible('.my-hint')
            .csVerifyMapScreenshot(PO.mapId(), 'hint')
            .moveToObject(PO.map.placemark.svgIconContent())
            .csVerifyMapScreenshot(PO.mapId(), 'anotherHint')
            .moveToObject(PO.map.controls.zoom.plus())
            .waitForInvisible('.my-hint')
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });

    it('Хинт повторно переоткрывается несколько раз', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.svgIcon())
            .moveToObject(PO.map.placemark.svgIcon())
            .waitForVisible('.my-hint')
            .moveToObject(PO.map.controls.zoom.plus())
            .waitForInvisible('.my-hint')
            .moveToObject(PO.map.placemark.svgIcon())
            .waitForVisible('.my-hint')
            .moveToObject(PO.map.controls.zoom.plus())
            .waitForInvisible('.my-hint')
            .moveToObject(PO.map.placemark.svgIcon())
            .waitForVisible('.my-hint')
            .moveToObject(PO.map.controls.zoom.plus())
            .waitForInvisible('.my-hint')
            .verifyNoErrors();
    });
});
