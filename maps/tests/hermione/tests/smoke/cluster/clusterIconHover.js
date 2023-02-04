describe('smoke/cluster/clusterIconHover.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/clusterIconHover.html')

            // Ждём карту
            .waitReady();
    });

    it('Кластер меняет свой цвет при наведении', function () {
        return this.browser
            .waitForVisible(cs.geoObject.cluster.smallInvertedIcon)
            .moveToObject(cs.geoObject.cluster.smallInvertedIcon)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'hoveredCluster')
            .verifyNoErrors();
    });

    it('Метка меняет свой цвет при наведении', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.svgIcon())
            .moveToObject(PO.map.placemark.svgIcon())
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'placemarkHovered')
            .verifyNoErrors();
    });

    it('Кликаем по кластеру чтобы проверить что опция DisableClickZoom выключена', function () {
        return this.browser
            .waitAndPointerClick(cs.geoObject.cluster.smallInvertedIcon)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'balloonCluster')
            .verifyNoErrors();
    });

    it('Наводимся на кластер, затем на метку, затем на кластер чтобы проверить что метки и кластер не пропадают', function () {
        return this.browser
            .waitForVisible(cs.geoObject.cluster.smallInvertedIcon)
            .moveToObject(cs.geoObject.cluster.smallInvertedIcon)
            .pause(200)
            .waitForVisible(PO.map.placemark.svgIcon())
            .moveToObject(PO.map.placemark.svgIcon())
            .pause(200)
            .waitForVisible(cs.geoObject.cluster.smallInvertedIcon)
            .moveToObject(cs.geoObject.cluster.smallInvertedIcon)
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'hover')
            .verifyNoErrors();
    });
});
