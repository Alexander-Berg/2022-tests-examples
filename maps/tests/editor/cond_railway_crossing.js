const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('railway crossing', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('can be created and changed correctly', function() {
        return createRoad.call(this.browser)
            .then(() => createRailwayCrossing.call(this.browser))
            .then(() => editRailwayCrossing.call(this.browser));
    });
});

function createRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(300, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(350, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(300, 250)
        .pause(HINT_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem5()) // cut
        .click(pageObject.ymapsItem5())
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 200, 300, 100, 'road-drawn')
        .saveGeoObject();
}

function createRailwayCrossing() {
    return this
        .createGeoObject('cond_railway_crossing')
        .debugLog('Putting railway crossing on rd_el')
        .waitForNotification('suggest-creating-cond-obstacle')
        .pointerClick(200, 250)
        .pause(DRAW_TIMEOUT)
        .waitForNotification('suggest-cond-obstacle-editor-set-from')
        .verifyMapScreenshot(100, 200, 300, 100, 'railway-crossing-drawn')
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'railway-crossing-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'railway-crossing-viewer');
}

function editRailwayCrossing() {
    return this
        .debugLog('Editing railway crossing...')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing railway crossing location and elements')
        .pointerClick(pageObject.redrawIcon())
        .waitForNotification('suggest-editing-cond-obstacle')
        .pointerClick(300, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .moveMouseAside()
        .verifyMapScreenshot(100, 200, 300, 100, 'railway-crossing-changed-drawn')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'railway-crossing-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'railway-crossing-changed-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'railway-crossing-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .closeSidebar()
}
