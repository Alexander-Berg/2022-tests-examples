describe('smoke/route/changeView.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/changeView.html')
            .waitReady()
            .waitForVisible(PO.routePoints.placemark());
    });

    it('Внешний вид первого кастомного маршрута', function () {
        return this.browser
            .pointerClick('ymaps=Первый маршрут')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'route1')
            .verifyNoErrors();
    });

    it('Внешний вид второго кастомного маршрута', function () {
        return this.browser
            .pointerClick('ymaps=Второй маршрут')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'route2')
            .verifyNoErrors();
    });

    it('Внешний вид балуна кастомного маршрута, по клику происходит переход на БЯК', function () {
        return this.browser
            .pointerClick('ymaps=Первый маршрут')
            .pause(500)
            .pointerClick(220, 369)
            .pause(1500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .pointerClick(PO.map.balloon.routeMore(), 10, 10)
            .verifyNoErrors()
            .getTabIds().then(function (res) {
                this.switchTab(res[1]);
            })
            .waitForVisible('div.route-form-view__travel-mode._mode_auto._checked')
            .close();
    });

    it('По клику по "Открыть маршрут" происходит переход на БЯК', function () {
        return this.browser
            .waitForVisible(PO.map.copyrights.promo())
            .pause(2000)
            .pointerClick(100, 490)
            .getTabIds().then(function (res) {
                this.switchTab(res[1]);
            })
            .waitForVisible('div.route-form-view__travel-mode._mode_auto._checked')
            .close();
    });
});
