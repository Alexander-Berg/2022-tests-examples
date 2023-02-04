describe('smoke/geoobject/editorIcon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/editorIcon.html')
            .waitReady()
            .waitForVisible(PO.map.pane.editor());
    });

    it('При наведении меняется иконка вершины', function () {
        return this.browser
            .moveToObject('body', 142, 227)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'edgeHover')
            .verifyNoErrors();
    });

    it('При наведении меняется иконка промежуточной точки', function () {
        return this.browser
            .moveToObject('body', 144, 162)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'vertexHover')
            .verifyNoErrors();
    });

    it('При перетаскивании меняется иконка вершины', function () {
        return this.browser
            .csDrag([142, 227], [162, 247])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'edgeDrag')
            .verifyNoErrors();
    });

    it('При перетаскивании меняется иконка промежуточной точки', function () {
        return this.browser
            .csDrag([144, 162], [164, 182])
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'vertexDrag')
            .verifyNoErrors();
    });

    it('При клике открывается панель и меняется иконка вершины', function () {
        return this.browser
            .pointerClick(142, 227)
            .pause(500)
            .csVerifyMapScreenshot(PO.mapId(), 'panel')
            .verifyNoErrors();
    });
});
