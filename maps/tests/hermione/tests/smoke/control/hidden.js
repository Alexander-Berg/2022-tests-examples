describe('smoke/control/hidden.html', function () {

    beforeEach(function(){
        return this.browser
            .openUrl('smoke/control/hidden.html')
            // Дожидаемся карты
            .waitForVisible('a=150px')
            .waitForVisible('a=220px')
            .waitForVisible('a=disable')
            .waitForVisible('a=Питер2');
    });

    it('Проверяем внешний вид контролов на очень маленькой карте', function () {
        return this.browser
            .pause(2000)
            .pointerClick(143, 30)
            .waitReady()
            .pause(200)
            .csVerifyMapScreenshot('body #tab-2', 'verySmallMap')
            .verifyNoErrors();
    });

    it('Проверяем внешний вид контролов на маленькой карте', function () {
        return this.browser
            .pause(2000)
            .pointerClick(244, 30)
            .waitReady()
            .pause(200)
            .csVerifyMapScreenshot('body #tab-3', 'smallMap')
            .verifyNoErrors();
    });

    it('Проверяем внешний вид контролов на средней карте', function () {
        return this.browser
            .pause(2000)
            .pointerClick(438, 30)
            .waitReady()
            .pause(200)
            .csVerifyMapScreenshot('body #tab-5', 'mediumMap')
            .verifyNoErrors();
    });

    it('Проверяем внешний вид контролов на большой карте', function () {
        return this.browser
            .pause(2000)
            .pointerClick(542, 30)
            .waitReady()
            .pause(200)
            .csVerifyMapScreenshot('body #tab-6', 'largeMap')
            .verifyNoErrors();
    });
});
