const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { ANIMATION_TIMEOUT, DRAW_TIMEOUT, HINT_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

let objectIndex = 0;

describe('vegetation contour', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('added', function() {
        return createVegetation.call(this.browser, 0)
            .then(() => createVegetation.call(this.browser, 150))
            .then(() => addContour.call(this.browser))
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-with-two-contours-on-map-selected')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-with-two-contours-on-map-deselected')

            .moveToObject('body', 200, 150)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 100, 350, 200, 'contour-belongs-to-both-vegetations-hint')
            .pointerClick(200, 150)
            .waitForVisible(pageObject.geoObjViewerView())
            .pause(ANIMATION_TIMEOUT)
            .waitForVisible(pageObject.moreIcon())
            .pointerClick(pageObject.moreIcon())
            .waitForVisible(pageObject.nkPopup())
            .selectElementByTextInMenu(i18n('common', 'share'))
            .waitForVisible(pageObject.shareView());
    });

    it('deleted', function() {
        return createVegetation.call(this.browser, 0)
            .then(() => createVegetation.call(this.browser, 150))
            .then(() => addContour.call(this.browser))

            .debugLog('Editing vegetation')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Deleting vegetation contour...')
            .moveToObject('body', 400, 150)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 125, 500, 175, 'vegetation-contour-choose-hint')
            .pointerClick(400, 150)
            .waitForVisible(pageObject.toolbarItem() + ' ' + pageObject.deleteIcon())
            .pointerClick(pageObject.toolbarItem() + ' ' + pageObject.deleteIcon())
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', 300, 200)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 125, 600, 175, 'vegetation-contour-revert-hint')
            .pointerClick(300, 200)
            .waitForVisible(pageObject.advancedMode())
            .pointerClick(pageObject.contourSelector())
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', 200, 150)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-another-contour-choose-hint')
            .pointerClick(200, 150)
            .waitForVisible(pageObject.toolbarItem() + ' ' + pageObject.deleteIcon())
            .pointerClick(pageObject.toolbarItem() + ' ' + pageObject.deleteIcon())
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-with-one-contour-drawn')
            .debugLog('Contour deleted')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-with-one-contour-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 350, 200, 'vegetation-with-one-contour-on-map-deselected');
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
        .pause(DRAW_TIMEOUT)
        .pointerClick(250 + shift, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150 + shift, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150 + shift, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .saveGeoObject();
}

function addContour() {
    return this
        .debugLog('Editing vegetation')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Adding vegetation contour...')
        .pointerClick(pageObject.editAddExternalContour())
        .waitForVisible(pageObject.contourSelectMode())
        .pointerClick(pageObject.contourSelectMode())
        .waitForNotification('suggest-select-slaves')
        .pointerClick(200, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Contour added')
        .saveGeoObject();
}
