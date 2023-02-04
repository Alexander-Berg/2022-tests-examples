describe('smoke/route/editing.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/editing.html')
            .waitReady()
            .waitForVisible(PO.routePoints.placemark());
    });

    it('Открывается балун на метке и неактивном маршруте при включенном рисовании', function () {
        return this.browser
            .pointerClick(PO.map.controls.button())
            .pause(500)
            .pointerClick(PO.routePoints.placemark())
            .waitForVisible(PO.map.balloon.closeButton())
            .pointerClick(242, 332)
            .waitForVisible(PO.map.balloon.closeButton())
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Виа точка появляется при наведении', function () {
        return this.browser
            .pointerClick(PO.map.controls.button())
            .pause(500)
            .moveToObject('body', 253, 256)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'via')
            .verifyNoErrors();
    });

    it('Добавляется путевая точка и драгается', function () {
        return this.browser
            .pointerClick(PO.map.controls.button())
            .pause(500)
            .pointerClick(273, 385)
            .waitForVisible(PO.routePoints.pinC())
            .csVerifyMapScreenshot(PO.mapId(), 'route1')
            .csDrag([273, 385], [165, 290])
            .moveToObject('body', 165, 290)
            .pause(3000)
            .csVerifyMapScreenshot(PO.mapId(), 'route2')
            .verifyNoErrors();
    });

    it('Удаляется путевая точка', function () {
        return this.browser
            .pointerClick(PO.map.controls.button())
            .waitForVisible(PO.routePoints.pinA())
            .pointerDblClick(PO.routePoints.pinA())
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'route4')
            .verifyNoErrors();
    });

    it('После выключения рисования, точки не добавляются и виа-точка не появляется', function () {
        return this.browser
            .waitForVisible(PO.routePoints.pinA())
            .waitForVisible(PO.routePoints.pinB())
            .pointerClick(PO.map.controls.button())
            .pause(500)
            .pointerClick(PO.map.controls.button())
            .pause(500)
            .pointerClick(273, 385)
            .pause(500)
            .moveToObject('body', 253, 256)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });
});
