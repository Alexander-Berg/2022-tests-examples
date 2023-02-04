describe('smoke/map/parameters.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/map/parameters.html', {tileMock: "withParameters"})
            .waitReady();
    });

    it('Изменим центр', function () {
        return this.browser
            .pointerClick('body #setCenter')
            .pause(3000)
            .csVerifyMapScreenshot(PO.mapId(), 'setCenter')
            .verifyNoErrors();
    });

    it('Изменим границы', function () {
        return this.browser
            .pointerClick('body #setBounds')
            .pause(3000)
            .csVerifyMapScreenshot(PO.mapId(), 'setBounds')
            .verifyNoErrors();
    });

    it('Изменим тип и плавно переместимся', function () {
        return this.browser
            .pointerClick('body #setTypeAndPan')
            .pause(3000)
            .csVerifyMapScreenshot(PO.mapId(), 'setTypeAndPan')
            .verifyNoErrors();
    });
});
