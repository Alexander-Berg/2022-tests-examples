const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants'),
    annotationAttrs = {
        type: 'cond_annotation-annotation_id__4' // smooth left turn
    };

require('../common.js')(beforeEach, afterEach);

describe('cond-annotation', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('can be created and changed correctly', function() {
        return createRoad.call(this.browser)
            .then(() => createAnnotation.call(this.browser))
            .then(() => editAnnotation.call(this.browser, annotationAttrs));
    });
});

function createRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(210, 210)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Cutting road')
        .pointerClick(210, 210)
        .pause(HINT_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem5()) // cut
        .click(pageObject.ymapsItem5())
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .saveGeoObject();
}

function createAnnotation() {
    return this
        .createGeoObject('cond_annotation')
        .debugLog('Drawing annotation geometry...')
        .waitForNotification('suggest-editing-cond')
        .pointerClick(210, 210)
        .waitForNotification('suggest-cond-editor-set-from')
        .pointerClick(175, 175)
        .waitForNotification('suggest-cond-editor-set-to')
        .pointerClick(225, 225)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .moveMouseAside()
        .verifyMapScreenshot(100, 100, 200, 200, 'annotation-drawn')
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'annotation-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'annotation-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'annotation-on-map-selected');
}

function editAnnotation(attrs) {
    return this
        .debugLog('Editing annotation')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing annotation id...')
        .pointerClick(pageObject.group1() + ' ' + pageObject.select.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', attrs.type))
        .debugLog('Id changed')

        .debugLog('Changing annotation geometry...')
        .pointerClick(pageObject.redrawIcon())
        .waitForNotification('suggest-editing-cond')
        .pointerClick(210, 210)
        .waitForNotification('suggest-cond-editor-set-from')
        .pointerClick(225, 225)
        .waitForNotification('suggest-cond-editor-set-to')
        .pointerClick(175, 175)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry changed')
        .verifyMapScreenshot(100, 100, 200, 200, 'annotation-changed-drawn')
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'annotation-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'annotation-changed-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'annotation-changed-on-map-selected')

        .debugLog('Verifying highlight on hover')
        .pointerClick(210, 210)
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .waitForVisible(pageObject.geoObjRelsCondAnnotation())
        .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-viewer')
        .moveToObject(pageObject.geoObjRelsViewItem.link())
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'annotation-changed-on-map-hover');
}
