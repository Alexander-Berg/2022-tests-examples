describe('balloon/packages.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/packages.html')
            .waitReady('body #logger');
    });

    it('Проверим загрузку пакета', function () {
        return this.browser
            .pause(1000)
            .csCheckText('body #logger', 'Parameters: #2.1.5/Map\n' +
                'Balloon package has been loaded')
            .verifyNoErrors();
    });
});