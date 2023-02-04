describe('balloon/setPosition.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/setPosition.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.balloon.closeButton());
    });

    it('Проверим окончательную позицию балуна после setPosition', function () {
        return this.browser
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .csCheckText('body #logger', 'Expected values - true\n' +
                'event: open, overlay: [object Object], isOpen: true\n' +
                'overlay: true\n' +
                'position: true')
            .verifyNoErrors();
    });
});