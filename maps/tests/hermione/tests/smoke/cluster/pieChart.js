describe('smoke/cluster/pieChart.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/pieChart.html')

            // Дожидаемся карты и кластеров
            .waitReady()
            .waitForVisible(cs.geoObject.cluster.pieChart);
    });

    it('Внешний вид пайчартов', function () {
        return this.browser
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'pieChart')
            .verifyNoErrors();
    });

    it('Внешний вид пайчартов после зума', function () {
        return this.browser
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'pieChartAfterZoom')
            .verifyNoErrors();
    });

    it('Пайчарт распадается при клике по нему', function () {
        return this.browser
            .waitForVisible(cs.geoObject.cluster.pieChart)
            .pointerClick(323, 359)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'pieChartAfterClick')
            .verifyNoErrors();
    });
});
