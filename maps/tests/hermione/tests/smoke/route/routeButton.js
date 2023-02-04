describe('smoke/route/routeButton.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/routeButton.html')
            .waitReady()
            .waitForVisible(PO.routePoints.placemark())
            .waitForVisible(PO.map.controls.routeButton.panel.svgB());
    });

    it('Панель открыта при запуске кейса', function () {
        return this.browser
            .waitForVisible(PO.routePoints.pinA())
            .csVerifyMapScreenshot(PO.mapId(), 'panel',{
                ignoreElements: [PO.map.controls.routeButton.panel.emptyInput()]
            })
            .verifyNoErrors();
    });

    it('Точка Б заполняется руками и строится маршрут', function () {
        return this.browser
            .waitAndClick(PO.map.controls.routeButton.panel.svgB())
            .pause(200)
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.routePoints.pinA())
            .waitForVisible(PO.routePoints.pinB())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'manual')
            .verifyNoErrors();
    });

    it('Точка Б заполняется по клику на карту и строится маршрут, данные из точки Б удаляются', function () {
        return this.browser
            .waitForVisible(PO.routePoints.pinA())
            .waitAndClick(PO.map.controls.routeButton.panel.svgB())
            .pause(500)
            .pointerClick(240, 227)
            .waitForVisible(PO.routePoints.pinA())
            .waitForVisible(PO.routePoints.pinB())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'click')
            .waitAndClick(PO.map.controls.routeButton.panel.clear())
            .waitForInvisible(PO.routePoints.pinB())
            .pointerClick(66, 24)
            .waitForInvisible(PO.map.controls.routeButton.panel.svgB())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutRoute')
            .verifyNoErrors();
    });

    it('Точки меняются местами при клике по кнопке', function () {
        return this.browser
            .waitForVisible(PO.routePoints.pinA())
            .waitForVisible("ymaps=улица Льва Толстого, 16")
            .pointerClick(PO.map.controls.routeButton.panel.svgB())
            .pointerClick(250, 227)
            .waitForVisible(PO.routePoints.pinA())
            .waitForVisible(PO.routePoints.pinB(), 20000)
            .pointerClick('ymaps=Поменять точки местами')
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'afterChange')
            .verifyNoErrors();
    });

    it('Строится ОТ и пешеходный маршрут', function () {
        return this.browser
            .pointerClick(PO.map.controls.routeButton.panel.routeType.mass())
            .pause(500)
            .pointerClick(250, 227)
            .waitForVisible(PO.routePoints.pinA())
            .waitForVisible(PO.routePoints.pinB())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'mass')
            .pointerClick(PO.map.controls.routeButton.panel.routeType.pedestrian())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'pedestrian')
            .verifyNoErrors();
    });
});
