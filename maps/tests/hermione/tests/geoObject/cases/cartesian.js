describe('geoobject/cartesian.html', function () {

    beforeEach(function () {
        return this.browser
            .openUrl('geoobject/cases/cartesian.html', false)
            .waitReady(PO.map.placemark.placemark());
    });

    it('Проверим, что у каждой геометрии есть балун', function () {
        return this.browser
            .pointerClick(205, 157)
            .waitForVisible(PO.map.balloon.content())
            .csCheckText(PO.map.balloon.content(), 'Content')
            .waitAndClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.content())
            
            .pointerClick(273, 161)
            .waitForVisible(PO.map.balloon.content())
            .csCheckText(PO.map.balloon.content(), 'Content')
            .waitAndClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.content())
            
            .pointerClick(337, 174)
            .waitForVisible(PO.map.balloon.content())
            .csCheckText(PO.map.balloon.content(), 'Content')
            .waitAndClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.content())
            
            .pointerClick(368, 267)
            .waitForVisible(PO.map.balloon.content())
            .csCheckText(PO.map.balloon.content(), 'Content')
            .waitAndClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.content())
            
            .pointerClick(272, 274)
            .waitForVisible(PO.map.balloon.content())
            .csCheckText(PO.map.balloon.content(), 'Content')
            .waitAndClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.content())
            
            .pointerClick(203, 271)
            .waitForVisible(PO.map.balloon.content())
            .csCheckText(PO.map.balloon.content(), 'Content')
            .waitAndClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.content())
            
            .pointerClick(132, 227)
            .waitForVisible(PO.map.balloon.content())
            .csCheckText(PO.map.balloon.content(), 'Content')
            .waitAndClick(PO.map.balloon.closeButton())
            .waitForInvisible(PO.map.balloon.content())
            
            .pointerClick(87, 276)
            .waitForVisible(PO.map.balloon.content())
            .csCheckText(PO.map.balloon.content(), 'Content')
            .pause(1500)
            .csVerifyScreenshot(PO.mapId(), 'balloon')
            .verifyNoErrors();
    });

    it('После драга и зума геометрии на месте', function () {
        return this.browser
            .csDrag([100,100], [200, 200])
            .pause(500)
            .pointerDblClick(385, 451)
            .pause(1500)
            .csVerifyMapScreenshot(PO.mapId(), 'afterZoom')
            .verifyNoErrors();
    });
});
