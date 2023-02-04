describe('balloon/autoPanReject.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/autoPanReject.html')
            .waitReady();
    });

    it('Проверим промисы', function () {
        return this.browser
            .pause(1000)
            .getText('body #logger').then(function (text) {
                text.should.equal('actionbegin\nactionend\nactionbegin\nactionend\nactionbegin\nactionend\n' +
                    'actionbegin\nactionend\nactionbegin\npanTo() fulfilled: true\npanTo() rejected: false\n' +
                    'actionend\nautoPan() fulfilled: true\nautoPan() rejected: false');
            })
            .verifyNoErrors();
    });
});