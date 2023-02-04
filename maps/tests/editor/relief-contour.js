const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants'),
    islandAttrs = {
        name: 'остров_автотест',
        nameLang: 'ru'
    },
    archipelagoAttrs = {
        name: 'архипелаг_автотест',
        nameLang: 'en',
        nameType: 'short',
        ftType: 'relief-ft_type_id__305'
    };

require('../common.js')(beforeEach, afterEach);

describe('relief-contour', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('has error', function() {
        return drawReliefContour.call(this.browser, islandAttrs)
            .verifyMapScreenshot(100, 100, 200, 200, 'relief-contour-bad-drawn')
            .debugLog('Trying to save relief with invalid geometry')
            .pointerClick(pageObject.geoObjEditorView.submit())
            .waitForVisible(pageObject.mapOverlayLayoutArrow())
            .waitForVisible(pageObject.notificationError())
            .pause(UPDATE_TIMEOUT)
            .verifyScreenshot(pageObject.notificationError(), 'relief-contour-error');
    });

    it('is drawn and changed', function() {
        return drawReliefContour.call(this.browser, islandAttrs)
            .debugLog('Deleting point and changing geometry to correct')
            .pointerClick(150, 150)
            .waitForVisible(pageObject.ymapsItem0())
            .pointerClick(pageObject.ymapsItem0())
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', 180, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(180, 150)
            .pause(UPDATE_TIMEOUT)
            .moveMouseAside()
            .verifyMapScreenshot(100, 100, 200, 200, 'relief-contour-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'relief-contour-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'relief-contour-on-map-selected')

            .debugLog('Editing relief')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing relief geometry...')
            .waitForNotification('suggest-edit-contour')
            .pointerClick(150, 250)
            .pause(DRAW_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem2()) // align angle
            .click(pageObject.ymapsItem2())
            .pause(DRAW_TIMEOUT)
            .pointerClick(175, 150)
            .pause(DRAW_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem3()) // round angle
            .click(pageObject.ymapsItem3())
            .pause(DRAW_TIMEOUT)
            .debugLog('Geometry changed')
            .verifyMapScreenshot(100, 100, 200, 200, 'relief-contour-changed-drawn')

            .debugLog('Changing relief attributes...')
            .selectFtType(archipelagoAttrs.ftType)
            .pointerClick(pageObject.textInputControl())
            .setValue(pageObject.textInputControl(), archipelagoAttrs.name)
            .selectNameAttrs(archipelagoAttrs.nameLang, archipelagoAttrs.nameType)
            .debugLog('Attributes changed')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'relief-contour-changed-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'relief-contour-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'relief-contour-changed-on-map-deselected');
    });
});

function drawReliefContour(attrs) {
    return this
        .createGeoObject('relief/ft_type_id__302') // island
        .verifyScreenshot(pageObject.geoObjEditorView(), 'relief-contour-editor-hint')

        .debugLog('Setting relief attributes...')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang)
        .debugLog('Attributes set')

        .debugLog('Drawing relief geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(180, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(UPDATE_TIMEOUT)
        .verifyScreenshot(pageObject.geoObjEditorView(), 'relief-contour-editor-no-hint')
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn');
}
