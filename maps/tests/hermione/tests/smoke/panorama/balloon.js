describe('smoke/panorama/balloon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/panorama/balloon.html')
            .waitReady()
            .waitForVisible(PO.map.placemark.placemark());
    });

    it('Проверяем появление панорамы в балуне, панорама в балуне драгается, балун закрывается без ошибок', function () {
        return this.browser
            .pointerClick(PO.map.placemark.placemark())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'panorama')
            .verifyNoErrors();
    });
});
