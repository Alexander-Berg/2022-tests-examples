describe('smoke/route/routeEditor.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/routeEditor.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Точки ставятся при включении редактора и перестают при выключении', function () {
        return this.browser
            .pointerClick('body #editor')
            .pause(500)
            .pointerClick(248, 336)
            .pause(500)
            .pointerClick(150, 341)
            .pause(500)
            .pointerClick('body #editor')
            .pause(500)
            .pointerClick(167, 457)
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route')
            .verifyNoErrors();
    });
});
