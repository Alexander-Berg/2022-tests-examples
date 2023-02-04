describe('smoke/geocode/inputValidation.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/inputValidation.html', 'white')
            // Дожидаемся инпута.
            .waitReady('body #suggest');

    });

    it('Проверяем точность ответа exact', function () {
        return this.browser
            .waitAndClick('body #suggest')
            .pause(500)
            .keys('Россия, Москва, улица Льва Толстого 16')
            .waitForVisible(PO.suggest())
            .pointerClick('body #button', 10, 10)
            .waitForVisible('body #messageHeader')
            .pause(1000)
            .csVerifyScreenshot(PO.pageResult(), 'result_exact')
            .verifyNoErrors();
    });

    it('Проверяем точность ответа street', function () {
        return this.browser
            .waitAndClick('body #suggest')
            .keys('Россия, Москва, улица Льва Толстого')
            .pause(1000)
            .pointerClick('body #button', 10, 10)
            .waitForVisible('body #notice')
            .pause(1600)
            .csVerifyScreenshot(PO.pageResult(), 'result_street')
            .verifyNoErrors();
    });

    it('Проверяем точность ответа near', function () {
        return this.browser
            .waitAndClick('body #suggest')
            .keys('Россия, Москва, улица Льва Толстого 29')
            .pointerClick('body #button', 10, 10)
            .waitForVisible('body #notice')
            .pause(1000)
            .csVerifyScreenshot(PO.pageResult(), 'result_near')
            .verifyNoErrors();
    });

    it('Невалидный запрос', function () {
        return this.browser
            .waitAndClick('body #suggest')
            .keys('fasdfadfasdfadsfadsfas')
            .pointerClick('body #button', 10, 10)
            .waitForVisible('body #notice')
            .pause(300)
            .csVerifyScreenshot(PO.pageResult(), 'result_bad')
            .verifyNoErrors();
    });
});
