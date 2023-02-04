describe('smoke/cluster/balloonCustomContent', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/balloonCustomContent.html')

            .waitReady()
            //Проверяем появление балуна с кастомным контентом на карте
            .waitForVisible('a=Заголовок метки №1');
    });

    it('Внешний вид кластеров с открытым балуном', function () {
        return this.browser
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('При наведении выделяется заголовок', function () {
        return this.browser
            .moveToObject('a=Заголовок метки №1')
            .csVerifyMapScreenshot(PO.mapId(), 'hoveredItem')
            .verifyNoErrors();
    });

    it('При клике на номер информация попадает в лог', function () {
        return this.browser
            .pointerClick('a=Заголовок метки №1')
            .moveToObject(PO.mapId())
            .csVerifyMapScreenshot(PO.mapId(), 'balloonItem')
            .csVerifyScreenshot(PO.pageLog(), 'log')
            .verifyNoErrors();
    });

    it('Балун закрывается на крестик', function () {
        return this.browser
            .waitAndClick(PO.map.balloon.closeButton())
            .csVerifyMapScreenshot(PO.mapId(), 'map')
            .verifyNoErrors();
    });

    it('Балун закрывается при зуме', function () {
        return this.browser
            .click(PO.map.controls.zoom.minus())
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'mapAfterZoom')
            .verifyNoErrors();
    });

    it('Балун закрывается при клике по другому кластеру', function () {
        return this.browser
            .waitAndPointerClick('ymaps=3')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAnotherCluster')
            .verifyNoErrors();
    });

    it('Открывается балун метки', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonPlacemark')
            .verifyNoErrors();
    });
});
