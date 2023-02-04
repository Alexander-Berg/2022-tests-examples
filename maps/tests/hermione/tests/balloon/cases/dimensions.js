describe('balloon/dimensions.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/dimensions.html', {tileMock: 'blueWithParameters'})
            .waitReady();
    });

    it('Проверяем опции высоты и ширины балуна', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .waitAndClick(PO.map.controls.button())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'changeData')
            .verifyNoErrors();
    });
});