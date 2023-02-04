describe('smoke/cluster/balloonAccordion.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/balloonAccordion.html')
            .waitReady()
            //Проверяем появление кластера на карте
            .waitForVisible(cs.geoObject.cluster.smallIcon);
    });

    it('Внешний вид кластеров с открытым балуном аккордеоном', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitForVisible(PO.map.balloon.accordionItemTitle())
            .pointerMoveTo(10, 10)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('При наведении выделяется заголовок метки', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitForVisible(PO.map.balloon.accordionItemCaption())
            .moveToObject(PO.map.balloon.accordionItemCaption())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'hoveredCaption')
            .verifyNoErrors();
    });

    it('При клике на заголовок открывается контент, при повторном клике закрывается', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitAndClick(PO.map.balloon.accordionItemTitle())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon_content')
            .waitAndClick(PO.map.balloon.accordionItemTitle())
            .moveToObject(PO.mapId(), 0, 0)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('Балун закрывается на крестик', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitAndClick(PO.map.balloon.accordionItemTitle())
            .pause(200)
            .waitAndClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutBalloon')
            .verifyNoErrors();
    });

    it('Балун закрывается при зуме', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitAndClick(PO.map.balloon.accordionItemTitle())
            .pause(500)
            .pointerDblClick(131, 287)
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutBalloonAfterZoom')
            .verifyNoErrors();
    });

    it('Балун закрывается при клике по другому кластеру', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitAndClick(PO.map.balloon.accordionItemTitle())
            .waitForVisible('ymaps=Метка №1')
            .pause(200)
            .pointerClick('ymaps=9')
            .waitForInvisible('ymaps=Метка №1')
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'anotherBalloon')
            .verifyNoErrors();
    });

    it('Открывается балун метки', function () {
        return this.browser
            .waitAndPointerClick(PO.map.placemark.placemark())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'placemark')
            .verifyNoErrors();
    });
});
