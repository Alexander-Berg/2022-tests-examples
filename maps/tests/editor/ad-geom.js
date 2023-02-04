const pageObject = require('../../page-object.js');
const { DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');
const ad1 = { name: 'еатд1_тестинг' };
const ad2 = { name: 'еатд2_тестинг' };

require('../common.js')(beforeEach, afterEach);

describe('ad', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('has simple contour', function() {
        return drawSimpleAd.call(this.browser, ad1)
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-simple-drawn')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'ad-simple-editor')
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'ad-simple-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-simple-on-map-selected')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-simple-on-map-deselected');
    });

    it('has complex contour and center', function() {
        return drawSimpleAd.call(this.browser, ad1)
            .saveGeoObject()
            .then(() => drawComplexAd.call(this.browser, ad2, true))
            .verifyMapScreenshot(100, 100, 175, 200, 'ad-complex-drawn')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'ad-complex-editor')
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'ad-complex-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-complex-on-map-selected')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-complex-on-map-deselected')
            .pointerClick(250, 225)
            .pause(UPDATE_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'ad-el-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-el-on-map-selected');
    });

    it('contours are added and deleted correctly', function() {
        return drawSimpleAd.call(this.browser, ad1)
            .saveGeoObject()
            .then(() => drawComplexAd.call(this.browser, ad2))
            .saveGeoObject()
            .then(() => editAdContours.call(this.browser, true))
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-contour-add-on-map-selected')
            .closeSidebar()
            .then(() => editAdContours.call(this.browser))
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-contour-delete-on-map-selected')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-contour-edit-on-map-deselected');
    });

    it('may have only center without contours', function() {
        return drawSimpleAd.call(this.browser, ad1, true)
            .saveGeoObject()
            .then(() => editAdContours.call(this.browser))
            .setMapCenterByTestNumber()
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-only-center-on-map-selected')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-only-center-on-map-deselected');
    });
});

function drawSimpleAd(obj, hasCenter) {
    return this
        .createGeoObject('ad')
        .debugLog('Drawing ad geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .then(() => hasCenter? addAdCenter.call(this) : true)

        .debugLog('Setting ad name as ' + obj.name)
        .pointerClick(pageObject.fieldsetCtrl1.input())
        .setValue(pageObject.fieldsetCtrl1.input(), obj.name)
        .selectNameAttrs('ru');
}

function drawComplexAd(obj, hasCenter) {
    return this
        .createGeoObject('ad')
        .debugLog('Drawing ad geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(200, 225)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 200)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 200)
        .pause(UPDATE_TIMEOUT)
        .pointerClick(250, 200)
        .pause(UPDATE_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem4()) // add new section
        .click(pageObject.ymapsItem4())
        .pointerClick(200, 225)
        .pause(DRAW_TIMEOUT)
        .pointerClick(200, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(pageObject.contourSelectMode())
        .pause(UPDATE_TIMEOUT)
        .moveToObject('body', 250, 250)
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 125, 250, 175, 'ad-complex-add-hint')
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .then(() => hasCenter? addAdCenter.call(this) : true)

        .debugLog('Setting ad name as ' + obj.name)
        .pointerClick(pageObject.fieldsetCtrl1.input())
        .setValue(pageObject.fieldsetCtrl1.input(), obj.name)
        .blurInput(pageObject.fieldsetCtrl1.input());
}

function editAdContours(toAdd) {
    return this
        .debugLog('Editing ad contours')
        .pointerClick(200, 150)
        .pause(UPDATE_TIMEOUT)
        .waitForVisible(pageObject.geoObjViewerView())
        .waitForVisible(pageObject.editIcon())

        .then(() => toAdd?
            this.debugLog('Adding ad contour')
                .pointerClick(pageObject.editIcon())
                .waitForVisible(pageObject.geoObjEditorView())
                .waitForInvisible(pageObject.editAddExternalContourDisabled())
                .moveMouseAside()
                .waitForVisible(pageObject.editAddExternalContour())
                .pointerClick(pageObject.editAddExternalContour())
                .pause(DRAW_TIMEOUT)
                .pointerClick(125, 125)
                .pause(DRAW_TIMEOUT)
                .pointerClick(275, 125)
                .pause(DRAW_TIMEOUT)
                .pointerClick(275, 275)
                .pause(DRAW_TIMEOUT)
                .pointerClick(125, 275)
                .pause(DRAW_TIMEOUT)
                .pointerClick(125, 125)
                .pause(DRAW_TIMEOUT)
                .saveGeoObject() :
            this.debugLog('Deleting ad contour')
                .pointerClick(pageObject.editIcon())
                .waitForVisible(pageObject.geoObjEditorView())
                .pointerClick(150, 150)
                .waitForVisible(pageObject.toolbarItem() + ' ' + pageObject.deleteIcon())
                .pointerClick(pageObject.toolbarItem() + ' ' + pageObject.deleteIcon()) // delete internal contour
                .pause(DRAW_TIMEOUT)
                .saveGeoObject()
        );
}

function addAdCenter() {
    return this
        .debugLog('Putting ad center...')
        .pointerClick(pageObject.addCenter())
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 225)
        .pause(DRAW_TIMEOUT)
        .debugLog('Center put');
}
