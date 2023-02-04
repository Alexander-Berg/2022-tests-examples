describe('balloon/MAPSAPI-10001.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/MAPSAPI-10001.html')
            .waitReady(PO.map.balloon.closeButton());
    });

    it('Проверяем методы балуна', function () {
        return this.browser
            .pointerClick(PO.map.controls.button())
            .pause(1000)
            .getText('body #logger').then(function (text) {
                text.should.equal('OK\nOK\nOK\nOK\nOK\nOK\nOK\nStateError: Popup is not captured at the moment\nOK\nOK\nOK\nStateError: Popup is not captured at the moment\nOK\nOK\nOK\nStateError: Popup is not captured at the moment\nOK');
            })
            .verifyNoErrors();
    });
});