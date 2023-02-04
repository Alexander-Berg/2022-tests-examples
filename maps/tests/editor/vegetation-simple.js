const pageObject = require('../../page-object.js');
const { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('vegetation', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('has error', function() {
        return createVegetation.call(this.browser, 182, 246)
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-error-drawn')
            .pointerClick(pageObject.geoObjEditorView.submit())
            .waitForVisible(pageObject.mapOverlayLayoutArrow())
            .waitForVisible(pageObject.notificationError())
            .pause(UPDATE_TIMEOUT)
            .verifyScreenshot(pageObject.notificationError(), 'vegetation-error-notification');
    });

    it('has simple contour and changes its contour geometry', function() {
        return createVegetation.call(this.browser)
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-simple-drawn')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'vegetation-simple-editor')
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'vegetation-simple-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-simple-on-map-selected')

            .debugLog('Editing vegetation')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing vegetation geometry...')
            .pointerClick(pageObject.alignAngles())
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-aligned-drawn')
            .pointerClick(170, 150)
            .pause(HINT_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem4()) // round all corners
            .click(pageObject.ymapsItem4())
            .pause(DRAW_TIMEOUT)
            .moveMouseAside()
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-rounded-drawn')
            .debugLog('Geometry changed')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-changed-on-map-deselected');
    });

    it('has center', function() {
        return createVegetation.call(this.browser)
            .pointerClick(pageObject.addCenter())
            .pause(DRAW_TIMEOUT)
            .pointerClick(175, 200)
            .pause(DRAW_TIMEOUT)

            .saveGeoObject()
            .moveMouseAside()
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-center-on-map-selected', { tolerance: 75 })

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-center-on-map-deselected', { tolerance: 75 });
    });
});

function createVegetation(x, y) {
    return this
        .createGeoObject('vegetation')
        .debugLog('Drawing vegetation geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(150, 245)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(170, 150)
        .pause(DRAW_TIMEOUT)
        .then(() => x?
            this.pointerClick(x, y) :
            this.pointerClick(150, 245)
        )
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn');
}
