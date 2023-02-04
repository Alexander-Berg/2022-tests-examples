describe('smoke/control/zoomLayout.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/control/zoomLayout.html')
            // Дожидаемся карты.
            .waitReady();
    });

    it('Проверяем что при клике меняется зум', function () {
        return this.browser
            .waitAndClick('.icon-plus')
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'zoom_in_map')
            .waitForVisible('.icon-minus')
            .moveToObject('.icon-minus')
            .buttonDown()
            .buttonUp()
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'zoom_out_map')
            .verifyNoErrors();
    });

    it('При наведении и клике зум-контрол меняет внешний вид', function () {
        return this.browser
            .waitForVisible('body #zoom-in')
            .moveToObject('body #zoom-in')
            .pause(200)
            .csVerifyScreenshot('body #zoom-in', 'zoom_in')
            .moveToObject('body #zoom-out')
            .pause(200)
            .csVerifyScreenshot('body #zoom-out', 'zoom_out')
            .moveToObject('body #zoom-out')
            .buttonDown()
            .pause(200)
            .csVerifyScreenshot('body #zoom-out', 'zoom_out_pressed')
            .verifyNoErrors();
    });
});
