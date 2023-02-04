describe('smoke/cluster/clusterCustomIcon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/cluster/clusterCustomIcon.html')

            .waitReady();
    });

    it('Разные кастомные иконки в зависимости от веса кластера', function () {
        return this.browser
            // Уменьшаем уровни зума чтобы появились большие кластеры
            .waitAndClick(PO.map.controls.zoom.minus())
            .pause(200)
            .waitAndClick(PO.map.controls.zoom.minus())

            // И проверяем что они действительно появились
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'clusters')
            .verifyNoErrors();
    });
});
