describe('smoke/map/checkVariable.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/map/checkVariable.html');
    });
    it('Не должно появиться новых глобальных переменных', function () {
        return this.browser
            .waitReady(PO.pageReady(), 40000)
            .getText('body #logger').then(function (text) {
                text.should.equal('Выполнено');
            })
            .verifyNoErrors();
    });
});
