const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants'),
    bldAttrs = {
        height: '3'
    };

require('../common.js')(beforeEach, afterEach);

describe('building copy', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is done with a hotkey ctrl+c', function() {
        return createBuilding.call(this.browser)
            .debugLog('Copying building')
            .keys(['Control', 'c'])
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'building-copy-editor')

            .debugLog('Dragging copy of building')
            .mouseDrag([225, 225], [275, 275])
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 250, 250, 'building-copy-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'building-copy-viewer')
            .verifyMapScreenshot(100, 100, 250, 250, 'building-copy-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 250, 250, 'building-copy-on-map-deselected');
    });
});

function createBuilding() {
    return this
        .createGeoObject('bld')
        .debugLog('Drawing building geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 100, 200, 200, 'building-drawn')

        .debugLog('Setting building height')
        .pointerClick(pageObject.inputControl.input()) // height
        .setValue(pageObject.inputControl.input(), bldAttrs.height)

        .saveGeoObject();
}
