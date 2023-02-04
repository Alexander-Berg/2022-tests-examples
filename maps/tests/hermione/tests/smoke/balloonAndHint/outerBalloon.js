describe('smoke/balloonAndHint/outerBalloon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/balloonAndHint/outerBalloon.html')
            .waitReady();
    });

    it('Балун выходит за пределы карты, если точка привязки во вьюпорте', function () {
        return this.browser
            .waitForVisible(PO.map.outerBalloon.closeButton())
            .csDrag([100, 100], [280, 120])
            // Задержка для смены типа инсепшна
            .pause(500)
            .csVerifyMapScreenshot('body', 'map')
            .csDrag([100, 100], [80, 100])
            // Задержка для смены типа инсепшна
            .pause(500)
            .csVerifyMapScreenshot('body', 'map2')
            .verifyNoErrors();
    });

    it('Балун ниже чем копирайты и линейка', function () {
        return this.browser
            .waitForVisible(PO.map.outerBalloon.closeButton())
            .csDrag([233, 148], [384, 421])
            .csVerifyMapScreenshot('body', 'map3')
            .verifyNoErrors();
    });
});
