describe('smoke/route/deliveryCalculator.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/route/deliveryCalculator.html')
            .waitReady();
    });

    it('Строится и перестраивается маршрут кликами по карте', function () {
        return this.browser
            .pointerClick(263, 399)
            .pause(500)
            .pointerClick(247, 398)
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'route')
            .pointerClick(109, 360)
            .pause(2000)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherRoute')
            .verifyNoErrors();
    });

    it('Строится маршрут через поля ввода', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .keys('Россия, Москва, улица Льва Толстого 16')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(296, 57)
            .waitForInvisible(PO.suggest.item0())
            .waitAndClick(PO.map.controls.search.large.button())
            .waitForVisible(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'routeA')
            .pointerClick(129, 58)
            .keys('Питер')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(296, 57)
            .waitForInvisible(PO.suggest.item0())
            .waitForVisible(PO.map.balloon())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'routeB')
            .verifyNoErrors();
    });
});
