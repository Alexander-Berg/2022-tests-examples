describe('smoke/geoobject/polygon.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('smoke/geoobject/polygon.html')

            //Появилась карта.
            .waitReady();
    });

    it('Проверяем балуны и хинты полигонов', function () {
        return this.browser
            //Открывается балун и хинт
            .pointerClick(321, 222)
            .waitForVisible(PO.map.balloon.content())
            .getText(PO.map.balloon.content()).then(function(text){
                text.should.equal('Многоугольник2')
            })
            .pointerClick(209, 222)
            .moveToObject('body', 209, 222)
            .waitForVisible(PO.map.hint.text())
            .getText(PO.map.hint.text()).then(function(text){
                text.should.equal('Многоугольник1')
            })
            .pointerClick(102, 221)
            .moveToObject('body', 102, 221)
            .waitForVisible(PO.map.hint.text())
            .getText(PO.map.hint.text()).then(function(text){
                text.should.equal('Многоугольник')
            })
            .pause(200)
            .csVerifyMapScreenshot(PO.mapId(), 'map_with_polygons')
            .verifyNoErrors();
    });

    it('После драга и зума нет ошибок в консоли и полигоны на месте', function () {
        return this.browser
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(200)
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(200)
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(200)
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(200)
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(200)
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(200)
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(200)
            .pointerClick(PO.map.controls.zoom.minus())
            .pause(200)
            .csDrag([100, 100],[200, 200])
            .pause(1000)
            .csVerifyScreenshot(PO.mapId(), 'map_with_polygons_after_zoom')
            .verifyNoErrors();
    });
});
