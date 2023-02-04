describe('balloon/autoPanCheckZoomRange.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('balloon/cases/autoPanCheckZoomRange.html', {tileMock: 'default'})
            .waitReady();
    });

    it('При автопане не подгружаются тайлы допустимого зума если autoPanCheckZoomRange по умолчанию', function () {
        return this.browser
            .waitAndClick("ymaps=[59, 30]")
            .pause(1000)
            .csVerifyMapScreenshot(PO.mapId(), 'autoPanCheckZoomRangeDefault');
    });

    it('При автопане подгружаются тайлы допустимого зума если autoPanCheckZoomRange установлена в true', function () {
        return this.browser
            .waitAndClick("ymaps=checkZoom")
            .waitAndClick("ymaps=[59, 30]")
            .csVerifyMapScreenshot(PO.mapId(), 'autoPanCheckZoomRangeTrue');
    });
});