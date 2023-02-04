describe('smoke/route/enterprise.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/enterprise.html')
            .waitReady();
    });

    it('Внешний вид маршрута ОТ в Москве', function () {
        return this.browser
            .pointerClick('ymaps=msc,mass')
            .waitForVisible(PO.routePoints.placemark())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route1')
            .verifyNoErrors();
    });

    it('Внешний вид маршрута авто в Москве', function () {
        return this.browser
            .pointerClick('ymaps=msc,auto')
            .waitForVisible(PO.routePoints.placemark())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route2')
            .verifyNoErrors();
    });

    it('Автобаундс не срабатывает для ОТ маршрута в Питере', function () {
        return this.browser
            .pointerClick('ymaps=spb,mass')
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route3')
            .verifyNoErrors();
    });

    it('Внешний вид маршрута Москва - СПБ', function () {
        return this.browser
            .pointerClick('ymaps=moscow,spb')
            .waitForVisible(PO.routePoints.placemark())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route4')
            .verifyNoErrors();
    });

    it('Внешний вид маршрута в Калининград', function () {
        return this.browser
            .pointerClick('ymaps=kaliningrad')
            .waitForVisible(PO.routePoints.placemark())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route5')
            .verifyNoErrors();
    });

    it('Внешний вид маршрута Минск - Киев', function () {
        return this.browser
            .pointerClick('ymaps=RUBK')
            .waitForVisible(PO.routePoints.placemark())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route6')
            .verifyNoErrors();
    });

    it('Внешний вид маршрута Париж - Лондон', function () {
        return this.browser
            .pointerClick('ymaps=enterprise,cities')
            .waitForVisible(PO.routePoints.placemark())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route7')
            .verifyNoErrors();
    });

    it('Внешний вид маршрута по координатам', function () {
        return this.browser
            .pointerClick('ymaps=enterprise,coords')
            .waitForVisible(PO.routePoints.placemark())
            .pause(4000)
            .csVerifyMapScreenshot(PO.mapId(), 'route8')
            .verifyNoErrors();
    });
});
