describe('smoke/geoQuery/applyBoundsClusterize.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoQuery/applyBoundsClusterize.html')

            .waitReady()
            .waitForVisible(cs.geoObject.cluster.smallIcon);
    });

    it('Кластеризованные результаты геокодирования', function () {
        return this.browser
            .pointerClick(130, 277)
            //Проверяем появление кластеризованных результатов геокодирования на карте
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_objects')
            .verifyNoErrors();
    });
});
