describe('geocode/getxAL.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('geocode/cases/getxAL.html', false)
            .waitReady()
            .waitForVisible('body #logger');
    });

    it('Проверим работу геттеров getxAL', function () {
        return this.browser
            .csCheckText('body #logger', 'Геокодирование \'Москва\'\nOK\nOK\nOK\nOK\nOK\nOK\nOK\nOK\nГеокодирование \'Танганьика\'\nOK\nOK\nOK\nOK\nOK\nOK\nOK\nOK\nГеокодирование \'аэропорт Шереметьево, терминал D\'\nOK\nOK\nOK\nOK\nOK\nOK\nOK\nOK');
    });
});
