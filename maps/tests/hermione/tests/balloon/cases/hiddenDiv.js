describe('balloon/hiddenDiv.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/hiddenDiv.html', {tileMock: 'blueWithParameters'})
            .waitReady('a=Москва');
    });

    it('Балун открывается на изначально скрытой карте', function () {
        return this.browser
            .pointerClick(190, 30)
            .waitForVisible(PO.map.balloon.closeButton())
            .pause(1000)
            .csVerifyMapScreenshot('body #tab-2', 'balloon');
    });
});