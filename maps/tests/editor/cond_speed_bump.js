const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('speed bump', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('can be created and changed correctly', function() {
        return createCrossroad.call(this.browser)
            .then(() => createSpeedBump.call(this.browser))
            .then(() => editSpeedBump.call(this.browser));
    });
});

function createCrossroad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing horizontal road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 350)
        .pause(DRAW_TIMEOUT)
        .pointerClick(350, 350)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .saveGeoObject()

        .createGeoObject('rd')
        .pointerClick(pageObject.geoObjEditorView.ftType())
        .waitForVisible(pageObject.nkPopup.menuFocused())
        .selectElementByTextInMenu(i18n('attr-values', 'rd_el-fc__10'))
        .waitForInvisible(pageObject.nkPopup.menuFocused())
        .pause(UPDATE_TIMEOUT)

        .debugLog('Drawing vertical pedestrian road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 450)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .saveGeoObject();
}

function createSpeedBump() {
    return this
        .createGeoObject('cond_speed_bump')
        .debugLog('Putting speed bump on rd_el')
        .waitForNotification('suggest-creating-cond-obstacle')
        .pointerClick(200, 350)
        .pause(DRAW_TIMEOUT)
        .waitForNotification('suggest-cond-obstacle-editor-set-from')
        .verifyMapScreenshot(100, 200, 300, 300, 'speed-bump-drawn')
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'speed-bump-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'speed-bump-viewer');
}

function editSpeedBump() {
    return this
        .debugLog('Editing speed bump...')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing speed bump location and elements')
        .pointerClick(pageObject.redrawIcon())
        .waitForNotification('suggest-editing-cond-obstacle')
        .pointerClick(250, 350)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 350)
        .pause(DRAW_TIMEOUT)
        .moveMouseAside()
        .verifyMapScreenshot(100, 200, 300, 300, 'speed-bump-changed-drawn')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'speed-bump-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'speed-bump-changed-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'speed-bump-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .closeSidebar()
}
