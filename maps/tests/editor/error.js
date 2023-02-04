const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants'),
    MAX_ZOOM = 20,
    errorAttrs = {
        type: 'error-type__18',
        status: 'error-status__4',
        tracker: 'NMAPS-777',
        desc: 'описание_автотест'
    };

require('../common.js')(beforeEach, afterEach);

describe('error', function() {
    beforeEach(function() {
        return this.browser
            .initNmaps('cartographer')
            .setMapCenterByTestNumber(MAX_ZOOM);
    });

    it('is drawn and changed', function() {
        return createError.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'error-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'error-on-map-selected')
            .then(() => editError.call(this.browser, errorAttrs));
    });
});

function createError() {
    return this
        .createGeoObjectBySuggest('error')
        .debugLog('Drawing error geometry...')
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

        .verifyScreenshot(pageObject.geoObjEditorView(), 'error-editor')
        .saveGeoObject();
}

function editError(attrs) {
    return this
        .debugLog('Editing error')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing error attributes...')
        .pointerClick(pageObject.control1() + ' ' + pageObject.select.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', attrs.type))
        .pointerClick(pageObject.control2() + ' ' + pageObject.select.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', attrs.status))
        .pointerClick(pageObject.inputControl.input())
        .setValue(pageObject.inputControl.input(), attrs.tracker)
        .pointerClick(pageObject.textarea())
        .setValue(pageObject.textarea(), attrs.desc)
        .debugLog('Attributes changed')

        .debugLog('Changing error geometry...')
        .mouseDrag([230, 230], [250, 250])
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry changed')
        .verifyMapScreenshot(100, 100, 200, 200, 'error-changed-drawn')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'error-changed-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'error-changed-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'error-changed-on-map-deselected');
}
