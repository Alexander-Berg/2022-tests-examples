const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

let objectIndex = 0;

describe('vegetation complex', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed with junction drag', function() {
        return createVegetation.call(this.browser, 0)
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-complex-drawn')
            .saveGeoObject()
            .moveMouseAside()
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-complex-on-map-selected')
            .then(() => createVegetation.call(this.browser, 150))
            .verifyMapScreenshot(100, 100, 350, 200, 'two-vegetations-drawn')
            .saveGeoObject()

            .debugLog('Editing vegetation')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Moving vegetation junction...')
            .waitForNotification('suggest-edit-contour')
            .moveToObject('body', 300, 150)
            .pause(UPDATE_TIMEOUT)
            .mouseDrag([300, 150], [250, 200])
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-changed-drawn')
            .debugLog('Junction moved')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-changed-on-map-deselected');
    });

    it('is changed in advanced mode', function() {
        return createVegetation.call(this.browser, 0)
            .saveGeoObject()
            .then(() => createVegetation.call(this.browser, 150))
            .saveGeoObject()

            .debugLog('Editing vegetation')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing vegetation contour...')
            .waitForInvisible(pageObject.advancedModeDisabled())
            .pointerClick(pageObject.advancedMode())
            .waitForNotification('suggest-draw-slaves')
            .pointerClick(300, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 150)
            .pause(UPDATE_TIMEOUT)
            .pointerClick(250, 150)
            .pause(HINT_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem3()) // add new contour section
            .click(pageObject.ymapsItem3())
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 250)
            .pause(DRAW_TIMEOUT)
            .pointerClick(300, 250)
            .pause(DRAW_TIMEOUT)
            .pointerClick(pageObject.contourSelectMode())
            .pause(DRAW_TIMEOUT)
            .pointerClick(400, 400)
            .pause(UPDATE_TIMEOUT)
            .moveToObject('body', 275, 150)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 125, 475, 175, 'vegetation-advanced-auto-hint')
            .moveToObject('body', 250, 200)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 125, 350, 175, 'vegetation-advanced-add-hint')
            .pointerClick(250, 200)
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', 300, 200)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 125, 350, 175, 'vegetation-advanced-delete-hint')
            .pointerClick(300, 200)
            .pause(DRAW_TIMEOUT)
            .moveMouseAside()
            .verifyMapScreenshot(100, 125, 350, 175, 'vegetation-advanced-drawn')
            .debugLog('Contour changed')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-advanced-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-advanced-on-map-deselected');
    });
});

function createVegetation(shift) {
    objectIndex++;

    return this
        .debugLog('Creating ' + objectIndex + ' vegetation')
        .createGeoObject('vegetation')

        .debugLog('Drawing vegetation geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(150 + shift, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250 + shift, 150)
        .pause(UPDATE_TIMEOUT)
        .pointerClick(250 + shift, 150)
        .pause(HINT_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem3()) // add new contour section
        .click(pageObject.ymapsItem3())
        .pointerClick(155 + shift, 155)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150 + shift, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250 + shift, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(245 + shift, 155)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn');
}
