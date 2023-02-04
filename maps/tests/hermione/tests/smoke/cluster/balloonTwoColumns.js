describe('smoke/cluster/balloonTwoColumns.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/balloonTwoColumns.html')

            .waitReady()
            .waitForVisible(cs.geoObject.cluster.smallIcon);
    });

    it('Внешний вид кластеров с открытым балуном две колонки', function () {
        return this.browser
            //Открываем балун кликом по кластеру
            .pointerClick(cs.geoObject.cluster.smallIcon)

            //Проверяем появление стандартного балуна
            .waitForVisible(PO.map.balloon.twoColumnsTabs())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('При наведении выделяется номер метки№6', function () {
        return this.browser
            //Открываем балун кликом по кластеру
            .pointerClick(cs.geoObject.cluster.smallIcon)

            //Проверяем появление стандартного балуна
            .waitForVisible(PO.map.balloon.twoColumnsTabs())
            .waitForVisible('ymaps=Метка №6')
            .pause(1000)
            .moveToObject('ymaps=Метка №6')
            .csVerifyMapScreenshot(PO.mapId(), 'hoveredItem')
            .verifyNoErrors();
    });

    it('При наведении не выделяется номер метки№1', function () {
        return this.browser
            //Открываем балун кликом по кластеру
            .pointerClick(cs.geoObject.cluster.smallIcon)

            //Проверяем появление стандартного балуна
            .waitForVisible(PO.map.balloon.twoColumnsTabs())
            .waitForVisible('ymaps=Метка №1')
            .moveToObject('ymaps=Метка №1')
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('При клике на номер открывается контент метки', function () {
        return this.browser
            //Открываем балун кликом по кластеру
            .pointerClick(cs.geoObject.cluster.smallIcon)

            //Проверяем появление стандартного балуна
            .waitForVisible(PO.map.balloon.twoColumnsTabs())
            .waitForVisible('ymaps=Метка №6')
            .pause(1000)
            .pointerClick('ymaps=Метка №6')
            .csVerifyMapScreenshot(PO.mapId(), 'balloonPlacemark6')
            .verifyNoErrors();
    });

    it('Балун закрывается на крестик', function () {
        return this.browser
            //Открываем балун кликом по кластеру
            .pointerClick(cs.geoObject.cluster.smallIcon)

            //Проверяем появление стандартного балуна
            .waitForVisible(PO.map.balloon.twoColumnsTabs())
            .waitAndClick(PO.map.balloon.closeButton())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'mapAfterClose')
            .verifyNoErrors();
    });

    it('Балун закрывается при зуме', function () {
        return this.browser
            //Открываем балун кликом по кластеру
            .pointerClick(cs.geoObject.cluster.smallIcon)

            //Проверяем появление стандартного балуна
            .waitForVisible(PO.map.balloon.twoColumnsTabs())
            .pause(500)
            .click(PO.map.controls.zoom.minus())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'mapAfterZoom')
            .verifyNoErrors();
    });

    it('Балун закрывается при клике по другому кластеру', function () {
        return this.browser
            //Открываем балун кликом по кластеру
            .pointerClick(cs.geoObject.cluster.smallIcon)

            //Проверяем появление стандартного балуна
            .waitForVisible(PO.map.balloon.twoColumnsTabs())
            .pause(500)
            .pointerClick('ymaps=8')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'mapAfterClick')
            .verifyNoErrors();
    });

    it('Открывается балун метки', function () {
        return this.browser
            //Открываем балун кликом по кластеру
            .pointerClick(cs.geoObject.cluster.smallIcon)

            //Проверяем появление стандартного балуна
            .waitForVisible(PO.map.balloon.twoColumnsTabs())
            .waitAndPointerClick('ymaps=9')
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonAnotherCluster')
            .verifyNoErrors();
    });
});
