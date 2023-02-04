const pageObject = require('../../page-object.js');
const { DRAW_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('circle road', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('can\'t be drawn due to small radius', function() {
        return this.browser
            .debugLog('Zoom in...')
            .pause(DRAW_TIMEOUT)
            .mouseDrag([200, 200], [300, 300], 1000, 'right')
            .pause(DRAW_TIMEOUT)

            .createGeoObject('rd')

            .debugLog('Starting to draw a circle road...')
            .pointerClick(pageObject.circle())
            .pause(DRAW_TIMEOUT)
            .mouseDrag([200, 200], [205, 205])
            .waitForVisible(pageObject.notificationError())
            .pause(DRAW_TIMEOUT)
            .waitForYMapsTilesLoaded()
            .verifyScreenshot(pageObject.notificationError(), 'circle-road-small-radius-error');
    });

    it('is drawn', function() {
        return drawRoad.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'circle-road-viewer')
            .pointerClick(200, 200)
            .pause(DRAW_TIMEOUT)
            .waitForVisible(pageObject.geoObjViewerView())
            .moveMouseAside()
            .verifyMapScreenshot(100, 100, 200, 200, 'half-circle-road-on-map-selected', { tolerance: 95 })
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'half-circle-road-on-map-deselected');
    });
});

function drawRoad() {
    return this
        .createGeoObject('rd')

        .debugLog('Starting to draw a road...')
        .pointerClick(pageObject.circle())
        .pause(DRAW_TIMEOUT)
        .mouseDrag([200, 200], [250, 250])
        .waitForInvisible(pageObject.submitDisabled())
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'circle-road-drawn', { tolerance: 95 })

        .saveGeoObject();
}
