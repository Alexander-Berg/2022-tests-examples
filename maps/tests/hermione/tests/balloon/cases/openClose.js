describe('balloon/openClose.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/openClose.html')
            .waitReady();
    });

    it('Проверим события', function () {
        return this.browser
            .pause(3000)
            .csCheckText('body #logger', 'Script: open, close, open, fire "userclose", destroy\n' +
                'event: open, overlay: [object Object], isOpen: true, target: [object Object]\n' +
                'event: close, overlay: null, isOpen: false, target: [object Object]\n' +
                'event: open, overlay: [object Object], isOpen: true, target: [object Object]\n' +
                'event: userclose, overlay: null, isOpen: false, target: [object Object]\n' +
                '[object Object]\n' +
                'event: close, overlay: null, isOpen: false, target: [object Object]\n' +
                'balloon destroyed\n' +
                'СОБЫТИЯ ПРАВИЛЬНЫЕ')
            .verifyNoErrors();
    });
});