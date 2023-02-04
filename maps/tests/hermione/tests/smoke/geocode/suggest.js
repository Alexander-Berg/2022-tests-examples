describe('smoke/geocode/suggest.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/suggest.html')
            .waitForVisible('body #suggest1')
            .waitForVisible('body #suggest2');
    });

    it('Саджест', function () {
        return this.browser
            // Вводим в первое поле запрос, выбираем результат
            .pause(500)
            .setValue('body #suggest1', 'кафе')
            .waitForVisible(PO.suggest())
            .csVerifyScreenshot(PO.pageResult(), 'suggest1',{
                ignoreElements: ['input#suggest1']
            })
            .verifyNoErrors();
    });
    it('Саджест по кастомным результатам', function () {
        return this.browser
            // Вводим во второе поле запрос и проверяем скриншотом
            .pause(500)
            .setValue('body #suggest2', 'ло')
            .waitForVisible(PO.suggest())
            .csVerifyScreenshot(PO.pageResult(), 'suggest2',{
                ignoreElements: ['input#suggest2']
            })
            .verifyNoErrors();
    });
});
