describe('balloon/properties.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/properties.html', {tileMock: 'blueWithParameters'})
            .waitReady();
    });

    it('Проверим установку properties', function () {
        return this.browser
            .pointerClick(100,100)
            .waitForVisible(PO.map.balloon.content())
            .pause(1000)
            .pointerClick(10,10)
            .waitForVisible(PO.map.balloon.content())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});