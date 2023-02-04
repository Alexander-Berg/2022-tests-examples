const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('fence', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('is drawn', function() {
        return drawObjects.call(this.browser)
            .createGeoObject('urban_roadnet_fence_el')
            .debugLog('Drawing fence geometry...')
            .waitForNotification('suggest-draw-geometry')
            .pointerClick(125, 125)
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', 150, 150)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'fence-snap-to-vegetation')
            .pointerClick(150, 150)
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', 175, 175)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'fence-snap-to-building')
            .pointerClick(175, 175)
            .pause(DRAW_TIMEOUT)
            .pointerClick(225, 175)
            .pause(DRAW_TIMEOUT)
            .pointerClick(225, 225)
            .pause(DRAW_TIMEOUT)
            .pointerClick(175, 225)
            .pause(DRAW_TIMEOUT)
            .pointerClick(125, 125)
            .pause(DRAW_TIMEOUT)
            .debugLog('Fence drawn')
            .moveMouseAside()
            .verifyMapScreenshot(100, 100, 200, 200, 'fence-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'fence-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'fence-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'fence-on-map-deselected');
    });
});

function drawObjects() {
    return this
        .createGeoObject('vegetation')
        .debugLog('Drawing vegetation geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Vegetation drawn')
        .moveMouseAside()
        .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-contour-drawn')
        .saveGeoObject()

        .createGeoObject('bld')
        .debugLog('Drawing building geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(175, 175)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 175)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 225)
        .pause(DRAW_TIMEOUT)
        .pointerClick(175, 225)
        .pause(DRAW_TIMEOUT)
        .debugLog('Building drawn')
        .verifyMapScreenshot(100, 100, 200, 200, 'building-drawn')
        .saveGeoObject();
}
