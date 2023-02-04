const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants'),
    bldAttrs = {
        ftType: 'bld-ft_type_id__106', // edifice
        state: 'bld-cond__1', // under construction
        height: '12'
    },
    bldAttrsChanged = {
        ftType: 'bld-ft_type_id__102', // industrial
        state: 'bld-cond__2', // abandoned
        height: '3'
    }
;

require('../common.js')(beforeEach, afterEach);

describe('building', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn by default', function() {
        return createBuilding.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'building-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'building-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-on-map-deselected');
    });

    it('is drawn with attributes and changed', function() {
        return createBuilding.call(this.browser, bldAttrs)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'building-with-attrs-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'building-with-attrs-on-map-selected')
            .then(() => editBuilding.call(this.browser, bldAttrsChanged));
    });
});

function createBuilding(attrs) {
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

        .then(() => attrs?
            this.debugLog('Setting building attributes...')
                .selectFtType(attrs.ftType)
                .pointerClick(pageObject.geoObjEditorView.condition())
                .waitForVisible(pageObject.nkPopup())
                .selectElementByTextInMenu(i18n('attr-values', attrs.state))
                .debugLog('Setting building height')
                .pointerClick(pageObject.inputControl.input())
                .setValue(pageObject.inputControl.input(), attrs.height)
                .debugLog('Attributes set')
                .verifyMapScreenshot(100, 100, 200, 200, 'building-with-attrs-drawn') :
            this.verifyMapScreenshot(100, 100, 200, 200, 'building-drawn')
        )
        .saveGeoObject();
}

function editBuilding(attrs) {
    return this
        .debugLog('Editing building')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing building attributes...')
        .pointerClick(pageObject.inputControl.input())
        .setValue(pageObject.inputControl.input(), attrs.height)
        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.geoObjEditorView.condition())
        .waitForVisible(pageObject.nkPopup.menuFocused())
        .selectElementByTextInMenu(i18n('attr-values', attrs.state))
        .debugLog('Attributes changed')

        .verifyScreenshot(pageObject.geoObjEditorView(), 'building-changed-editor')
        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'building-changed-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'building-changed-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'building-changed-on-map-deselected');
}
