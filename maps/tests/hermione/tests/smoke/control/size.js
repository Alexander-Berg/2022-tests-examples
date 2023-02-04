describe('smoke/control/size.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/control/size.html')
            // Ждём карту.
            .waitReady();
    });

    it('Проверяем базовую работу большого контрола поиска', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.map.balloon.closeButton())
            .pointerClick(PO.map.controls.search.large.input())
            .pointerClick(PO.map.controls.search.large.list())
            .waitForVisible(PO.map.controls.search.large.serp.panel())
            .waitForVisible(PO.map.controls.search.large.serp.item())

            // Проверяем балун и результаты поиска скриншотом
            .waitForVisible(PO.map.balloon.closeButton())
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'result')
            .verifyNoErrors();
    });

    it('Проверяем внешний вид контролов поиска после перехода в фулскрин', function () {
        return this.browser
            .waitAndClick(PO.map.controls.fullscreen())
            .pause(100)
            .csVerifyMapScreenshot(PO.map.pane.events(), 'fullscreen')
            .verifyNoErrors();
    });


    it('Проверяем частичное совпадение саджеста', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .keys('др')
            .waitForVisible(PO.suggest.item0())
            .csVerifyMapScreenshot(PO.mapId(), 'result2',{
                ignoreElements: [PO.map.controls.search.large.input()]
            })
            .verifyNoErrors();
    });
});
