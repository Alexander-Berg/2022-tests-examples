describe('balloon/balloonLayout.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/balloonLayout.html', {tileMock: 'blueWithParameters'})
            .waitReady();
    });

    it('Проверяем появление балуна при загрузке', function () {
        return this.browser
            .getText('body #logger').then(function (text) {
                text.should.equal('event: open, overlay: [object Object], isOpen: true, target: [object Object]');
            })
            .csVerifyMapScreenshot(PO.mapId(), 'layout')
            .verifyNoErrors();
    });

    it('Балун закрывется и открывается при клике', function () {
        return this.browser
            .waitAndClick('body #close')
            .waitForInvisible('body #close')
            .waitForInvisible('.arrow')
            .pointerClick(150, 150)
            .csDrag([100,100], [200,200])
            .getText('body #logger').then(function (text) {
                text.should.equal('event: open, overlay: [object Object], isOpen: true, target: [object Object]\n' +
                    'event: userclose, overlay: [object Object], isOpen: true, target: [object Object]\n' +
                    'event: close, overlay: null, isOpen: false, target: [object Object]\n' +
                    'event: open, overlay: [object Object], isOpen: true, target: [object Object]');
            })
            .csVerifyMapScreenshot(PO.mapId(), 'layoutAfterClick')
            .verifyNoErrors();
    });
});