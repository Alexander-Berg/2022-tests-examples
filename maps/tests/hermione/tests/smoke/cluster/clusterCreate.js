describe('smoke/cluster/clusterCreate.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/clusterCreate.html')

            // Подождём карту
            .waitReady();
    });

    it('Проверка preset, disableClickZoom, hideIconOnBalloonOpen', function () {
        return this.browser
            .waitForVisible(PO.map.placemark.placemark())
            .pause(1000)
            .pointerClick(PO.map.placemark.placemark())
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'placemark_balloon')

            // Проверяем что при открытии балуна не срабатывает зум и не скрывается кластер и цвет иконок кластера
            .pointerClick(cs.geoObject.cluster.smallInvertedIcon)
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });
});
