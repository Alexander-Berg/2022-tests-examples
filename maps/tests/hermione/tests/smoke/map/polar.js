describe('smoke/map/polar.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/map/polar.html')
            .waitReady()
            .waitForVisible('ymaps=Добавить регионы');
    });

    it('Призумимся и драгнем', function () {
        return this.browser
            .csDrag([100, 100],[300, 300])
            .pause(500)
            .waitAndClick(PO.map.controls.zoom.plus())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'dragAndZoom')
            .verifyNoErrors();
    });

    it('Добавим регионы и проверим хинт', function () {
        return this.browser
            .pointerClick('ymaps=Добавить регионы')
            .waitForVisible(PO.map.pane.areas())
            .moveToObject('body', 300, 250)
            .waitForVisible(PO.map.hint())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'hintAndRegions')
            .verifyNoErrors();
    });

    it('Включим антарктику и регионы', function () {
        return this.browser
            .pause(500)
            .waitAndClick(PO.map.controls.zoom.minus())
            .pause(200)
            .pointerClick('ymaps=Добавить регионы')
            .pointerClick('ymaps=Антарктика')
            .waitForVisible(PO.map.pane.areas())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'antarctic')
            .verifyNoErrors();
    });

    it('Добавим линейку и найдем Москву', function () {
        return this.browser
            .waitAndClick(PO.mapControlsButtonText() + '=Найти')
            .waitAndClick(PO.searchPanel.input())
            .keys('Москва')
            .waitAndClick(PO.searchPanel.button())
            .waitForVisible(PO.map.balloon.closeButton())
            .pause(1000)
            .pointerClick(PO.map.controls.ruler())
            .pointerClick(453, 233)
            .pointerClick(417, 392)
            .pointerClick(317, 200)
            .pointerClick(484, 325)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'rulerWithMoscow')
            .verifyNoErrors();
    });
});
