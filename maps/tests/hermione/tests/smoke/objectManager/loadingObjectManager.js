describe('smoke/objectManager/loadingObjectManager.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/objectManager/loadingObjectManager.html')
            .waitReady()
            .waitForVisible("ymaps=Очень длиннный, но невероятно интересный текс.");
    });

    it('При переходе в фулскрин дозагружаются данные', function () {
        return this.browser
            .pointerClick(PO.map.controls.fullscreen())
            .pause(2000)
            .csVerifyMapScreenshot(PO.map.pane.events(), 'geoobjects')
            .verifyNoErrors();
    });

    it('Зум и драг не ломает LOM', function () {
        return this.browser
            .csDrag([20, 40], [460,40])
            .pause(500)
            .pointerDblClick(40, 40)
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherGeoobjects')
            .verifyNoErrors();
    });

    it('Работает фильтрация по координатам', function () {
        return this.browser
            .waitAndClick('ymaps=coords[1] > 34 & coords[1] < 38')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'coordsFilter')
            .verifyNoErrors();
    });

    it('Работает фильтрация по кастомному полю', function () {
        return this.browser
            .waitAndClick('ymaps=type < аптека')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'customFilter')
            .verifyNoErrors();
    });

    it('Работает фильтрация по пресету', function () {
        return this.browser
            .waitAndClick('ymaps=preset == yellow')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'presetFilter')
            .verifyNoErrors();
    });

    it('Возвращается фильтр', function () {
        return this.browser
            .waitAndClick('ymaps=getfilter')
            .waitAndClick('ymaps=preset == yellow')
            .waitAndClick('ymaps=getfilter')
            .pause(500)
            .csVerifyScreenshot(PO.pageLog(), 'log')
            .verifyNoErrors();
    });
});
