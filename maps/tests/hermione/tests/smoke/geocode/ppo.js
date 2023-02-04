describe.skip('smoke/geocode/ppo.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geocode/ppo.html', {domain: 'net'})

            // Ждём карту
            .waitReady();
    });

    it('Базовое ППО', function () {
        return this.browser
            .waitForVisible(PO.map.controls.button())

            // Проверяем наличие меток на карте и наличие панели результатов поиска
            .waitForVisible(PO.map.placemark.placemark())
            .waitForVisible(PO.map.controls.search.large.serp.panel())
            .waitForVisible(PO.map.controls.search.large.serp.item())

            // Удаляем результаты поиска, проверяем объектный саджест скриншотом
            .pointerClick(PO.map.controls.search.large.clear())
            .pointerClick(PO.map.map())
            .pointerClick(PO.map.controls.search.large.input())
            .waitForVisible(PO.suggest.catalogItem())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'suggest',{
                ignoreElements: [PO.map.controls.search.large.input()]
            })

            // Проверяем что по клику на объектный саджест на карте появляются метки и панель результатов поиска
            .pointerClick(PO.suggest.catalogItem())
            .waitForVisible(PO.map.controls.search.large.serp.panel())
            //.waitForVisible(PO.map.controls.search.large.serp.advert())
            .waitForVisible(PO.map.controls.search.large.serp.item())

            // Удаляем результаты поиска, проверяем саджест "кафе"
            .pointerClick(PO.map.controls.search.large.clear())
            .setValue(PO.map.controls.search.large.input(), 'кафе')
            .waitForVisible(PO.suggest())
            .moveToObject(PO.suggest())
            .csVerifyMapScreenshot(PO.mapId(), 'another_suggest',{
                ignoreElements: [PO.map.controls.search.large.input()]
            })

            // Ищём кафе Фонда, открываем результаты поиска
            .pointerClick(PO.map.controls.search.large.clear())
            .setValue(PO.map.controls.search.large.input(), 'Шахин-Шах')
            .pointerClick(PO.map.controls.search.large.button())
            .waitForVisible(PO.map.balloon.closeButton())
            .pointerClick(PO.map.controls.search.large.input())
            .pointerClick(PO.map.controls.search.large.list())
            .waitForVisible(PO.map.controls.search.large.serp.panel())
            .waitForVisible(PO.map.controls.search.large.serp.item())

            // Проверяем балун ППО и результаты поиска скриншотом
            .waitForVisible(PO.map.balloon.closeButton())
            .pause(100)
            .csVerifyMapScreenshot(PO.mapId(), 'result', {
                ignoreElements: [PO.map.statusTime()]
            })
            .verifyNoErrors();
    });
});
