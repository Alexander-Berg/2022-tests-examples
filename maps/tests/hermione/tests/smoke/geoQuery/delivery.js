describe('smoke/geoQuery/delivery.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoQuery/delivery.html')
            .waitReady();
    });

    it('Открывается балун на полигоне', function () {
        return this.browser
            .pointerClick(263, 232)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('После поиска и перетаскиванияметка меняет свой цвет и выделяется полигон', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .keys('Санкт-Петербург')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_search')
            .csDrag([268, 203],[280, 105])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_drag')
            .verifyNoErrors();
    });

    it('Чёрная метка', function () {
        return this.browser
            .waitAndClick(PO.map.controls.search.large.input())
            .keys('Москва')
            .waitForVisible(PO.suggest.item0())
            .pointerClick(PO.suggest.item0())
            .waitForVisible(PO.map.placemark.placemark())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'after_false_search')
            .verifyNoErrors();
    });
});
