const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { ANIMATION_TIMEOUT, DRAW_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('road', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('should set and display 1-4 ft-type correctly', function() {
        return createRoadFtTypeRange.call(this.browser, 1, 4);
    });

    it ('should set and display 5-7 ft-type correctly', function() {
        return createRoadFtTypeRange.call(this.browser, 5, 7);
    });

    it('should set and display 8-10 ft-type correctly', function() {
        return createRoadFtTypeRange.call(this.browser, 8, 10);
    });
});

function createRoadFtTypeRange(firstFtType, lastFtType) {
    let browser = this;
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .pointerClick(pageObject.geoObjEditorView.ftType())
        .waitForVisible(pageObject.nkPopup.menuFocused())
        .then(() => {
            if(firstFtType === 1) {
                return this
                    .pause(ANIMATION_TIMEOUT)
                    .verifyScreenshot(pageObject.nkPopup.menuFocused(), 'ft-type-popup');
            }
            return true;
        })
        .selectElementByTextInMenu(i18n('attr-values', 'rd_el-fc__' + firstFtType))
        .waitForInvisible(pageObject.nkPopup.menuFocused())
        .pause(ANIMATION_TIMEOUT)
        .verifyScreenshot(pageObject.geoObjEditorView(), 'editor-ft-type-' + firstFtType)

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'viewer-ft-type-' + firstFtType)
        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'ft-type-' + firstFtType)

        .then(() => {
            for(let i = firstFtType + 1; i <= lastFtType; i++) {
                browser = changeRoadFtType.call(browser, i);
            }
            return browser;
        });
}

function changeRoadFtType(itemIndex) {
    return this
        .pointerClick(200, 200)
        .waitForVisible(pageObject.geoObjViewerView())
        .waitForVisible(pageObject.editIcon())
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())
        .pointerClick(pageObject.geoObjEditorView.ftType())
        .waitForVisible(pageObject.nkPopup.menuFocused())
        .selectElementByTextInMenu(i18n('attr-values', 'rd_el-fc__' + itemIndex))
        .waitForInvisible(pageObject.nkPopup.menuFocused())
        .pause(ANIMATION_TIMEOUT)
        .verifyScreenshot(pageObject.geoObjEditorView(), 'editor-ft-type-' + itemIndex)

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'viewer-ft-type-' + itemIndex)
        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'ft-type-' + itemIndex);
}
