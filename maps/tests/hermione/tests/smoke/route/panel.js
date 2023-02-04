describe('smoke/route/panel.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/panel.html')
            .waitReady();
    });

    it('По клику по карте строится маршрут, после нажатия на кнопку точки меняются местами', function () {
        return this.browser
            .waitForVisible(PO.routePoints.pinA())
            .waitAndClick(PO.map.controls.routePanel.emptyInput())
            .pointerClick(242, 227)
            .waitForVisible(PO.routePoints.pinB())
            .waitForVisible(PO.map.controls.routePanel.clear())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'click')
            .pointerClick(PO.map.controls.button())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'reverseRoute')
            .verifyNoErrors();
    });

    it('Руками заполняется поле и строится маршрут', function () {
        return this.browser
            .waitAndClick(PO.map.controls.routePanel.svgB())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'manual')
            .verifyNoErrors();
    });

    it('Строится пешеходный маршрут', function () {
        return this.browser
            .pointerClick(PO.map.controls.routePanel.routeType.pedestrian())
            .waitAndClick(PO.map.controls.routePanel.svgB())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.routePoints.pedestrianPin())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'pedestrian')
            .verifyNoErrors();
    });
});
