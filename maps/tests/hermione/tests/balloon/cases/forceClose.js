describe('balloon/forceClose.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/forceClose.html', {tileMock: 'blueWithParameters'})
            .waitReady()
            .waitForVisible(PO.map.balloon.closeButton());
    });

    it('Форсированное закрытие балуна', function () {
        return this.browser
            .pointerClick('ymaps=force')
            .csVerifyMapScreenshot(PO.mapId(), 'forceCloseBalloon')
            .verifyNoErrors();
    });

    it('Не форсированное закрытие балуна', function () {
        return this.browser
            .pointerClick('ymaps=close')
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .waitForInvisible(PO.map.balloon.closeButton())
            .verifyNoErrors();
    });

    it('Проверяем промис', function () {
        return this.browser
            .pointerClick('ymaps=promise')
            .getText('body #logger').then(function (text) {
                text.should.equal('event: open, overlay: [object Object], isOpen: true, target: [object Object]\n' +
                    'event: close, overlay: null, isOpen: false, target: [object Object]\n' +
                    'resolved: true, fulfilled: false, rejected: true');
            })
            .verifyNoErrors();
    });
});