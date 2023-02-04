describe('smoke/map/changeSize.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/map/changeSize.html')
            .waitReady();
    });

    it('Карта должна правильно реагировать на изменение размера контейнера', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.small())
            .waitForVisible(PO.map.searchPanel.fold())
            .pause(1000)
            .pointerClick(PO.map.map())
            //Поисковый контрол открылся
            .csVerifyMapScreenshot(PO.mapId(), 'before_expand')

            .waitAndClick('body #toggler')
            .pause(100)
            //Карта не должна отреагировать на изменение размера контейнера
            .csVerifyMapScreenshot(PO.mapId(), 'after_expand')

            .waitAndClick('body #checkbox')
            .pause(100)
            //Карта не должна отреагировать на изменение размера контейнера
            .csVerifyMapScreenshot(PO.mapId(), 'after_expand')

            .waitAndClick('body #toggler')
            //Карта не должна отреагировать на изменение размера контейнера
            .csVerifyMapScreenshot(PO.mapId(), 'before_expand')

            .waitAndClick('body #toggler')
            .pause(100)
            //Карта должна отреагировать на изменение размера контейнера
            .csVerifyMapScreenshot(PO.mapId(), 'after_expand_with_fitToViewport')
            .verifyNoErrors();
    });
});
