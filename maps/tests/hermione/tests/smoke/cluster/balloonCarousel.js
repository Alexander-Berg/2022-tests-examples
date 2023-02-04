describe('smoke/cluster/balloonCarousel.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/balloonCarousel.html')
            .waitReady()
            //Проверяем появление балуна карусели на карте
            .waitForVisible(cs.geoObject.cluster.smallIcon);
    });

    it('Внешний вид кластеров с открытым балуном каруселью', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitForVisible(PO.map.balloon.carouselNext())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('При наведении выделяется номер', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitForVisible(PO.map.balloon.carouselNext())
            .moveToObject(PO.map.balloon.carouselPagerItem())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'hoveredPagerItem')
            .verifyNoErrors();
    });

    it('При наведении выделяется стрелка', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitForVisible(PO.map.balloon.carouselNext())
            .moveToObject(PO.map.balloon.carouselNext())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'hoveredNext')
            .verifyNoErrors();
    });

    it('По клику на стрелку открывается контент следующей метки и при клике на номер открывается контент первой метки', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitAndClick(PO.map.balloon.carouselNext())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'contentNext')
            .waitAndClick(PO.map.balloon.carouselPagerItem())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'contentPagerItem')
            .verifyNoErrors();
    });

    it('Троеточие выделяется, по клику на троеточие открываются следующие метки', function () {
        return this.browser
            .pointerClick('ymaps=9')
            .waitForVisible(PO.map.balloon.carouselNext())
            .pause(500)
            .pointerClick(PO.map.balloon.carouselNext())
            .pause(500)
            .pointerClick(PO.map.balloon.carouselNext())
            .pause(500)
            .pointerClick(PO.map.balloon.carouselNext())
            .pause(500)
            .pointerClick(PO.map.balloon.carouselNext())
            .pause(500)
            .pointerClick(PO.map.balloon.carouselNext())
            .pause(500)
            .moveToObject(PO.map.balloon.carouselEllipsis())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'hoveredEllipsis')
            .pointerClick(PO.map.balloon.carouselEllipsis())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'contentEllipsis')
            .verifyNoErrors();
    });

    it('Балун закрывается на крестик', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitForVisible(PO.map.balloon.carouselNext())
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
            .waitForVisible(PO.map.balloon.carouselNext())
            .pause(200)
            .pointerDblClick(131, 287)
            .waitForInvisible(PO.map.balloon.closeButton())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'withoutBalloonAfterZoom')
            .verifyNoErrors();
    });

    it('Балун закрывается при клике по другой метке', function () {
        return this.browser
            .pointerClick(cs.geoObject.cluster.smallIcon)
            .waitForVisible(PO.map.balloon.carouselNext())
            .pause(200)
            .pointerClick('ymaps=9')
            .waitForInvisible('h2=Метка №1')
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
