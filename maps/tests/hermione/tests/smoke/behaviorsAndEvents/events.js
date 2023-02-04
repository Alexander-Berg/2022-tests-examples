describe('smoke/behaviorsAndEvents/events.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/behaviorsAndEvents/events.html')
            .waitReady('body #myElement')
            .waitForVisible('body #log');
    });

    it('Наведение - клик - сведение', function () {
        return this.browser
            .pause(1500)
            .moveToObject('body #myElement')
            .pointerClick('body #myElement', 10, 10)
            .moveToObject('body #log')
            .verifyNoErrors()
            .getText('body #logger').then(function (text) {
                text.should.equal('mouseenter\nclick\nmouseleave');

            });
    });

    it('Наведение - даблклик - сведение', function () {
        return this.browser
            .pause(500)
            .moveToObject('body #myElement')
            .pointerDblClick('body #myElement')
            .moveToObject('body #log')
            .verifyNoErrors()
            .getText('body #logger').then(function (text) {
                text.should.equal('mouseenter\nclick\nclick\ndblclick\nmouseleave');
            });
    });

    it('Наведение - сведение', function () {
        return this.browser
            .pause(500)
            .moveToObject('body #myElement')
            .moveToObject('body #log')
            .getText('body #logger').then(function (text) {
                text.should.equal('mouseenter\nmouseleave');
            })
            .verifyNoErrors();
    });
});
