const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { COMMENTS_TIMEOUT, DRAW_TIMEOUT, HINT_TIMEOUT } = require('../../tools/constants'),
    roadAttrs = {
        firstAddrType: 'rd_el-addr_scheme__odd',
        firstAddrStart: '1',
        firstAddrFinish: '13',
        secondAddrType: 'rd_el-addr_scheme__mixed',
        secondAddrStart: '14',
        secondAddrFinish: '23'
    };

require('../common.js')(beforeEach, afterEach);

describe('addr scheme', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('correlates to road element length', function() {
        return createRoad.call(this.browser, roadAttrs)
            .pointerClick(170, 170)
            .pause(DRAW_TIMEOUT)
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .pause(COMMENTS_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'road-short-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'road-short-on-map-selected')
            .closeSidebar();
    });
});

function createRoad(attrs) {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(180, 180)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)

        .debugLog('Setting road attributes...')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'road-editor')
        .pointerClick(pageObject.group4() + ' ' + pageObject.control2() + ' ' + pageObject.input())
        .setValue(pageObject.group4() + ' ' + pageObject.control2() + ' ' + pageObject.input(), attrs.secondAddrStart)
        .pointerClick(pageObject.group4() + ' ' + pageObject.control3() + ' ' + pageObject.input())
        .setValue(pageObject.group4() + ' ' + pageObject.control3() + ' ' + pageObject.input(), attrs.secondAddrFinish)
        .pointerClick(pageObject.group3() + ' ' + pageObject.control1() + ' ' + pageObject.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', attrs.firstAddrType))
        .pointerClick(pageObject.group3() + ' ' + pageObject.control2() + ' ' + pageObject.input())
        .setValue(pageObject.group3() + ' ' + pageObject.control2() + ' ' + pageObject.input(), attrs.firstAddrStart)
        .pointerClick(pageObject.group3() + ' ' + pageObject.control3() + ' ' + pageObject.input())
        .setValue(pageObject.group3() + ' ' + pageObject.control3() + ' ' + pageObject.input(), attrs.firstAddrFinish)
        .pointerClick(pageObject.group4() + ' ' + pageObject.select.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', attrs.secondAddrType))
        .debugLog('Attributes set')

        .debugLog('Cutting road')
        .pointerClick(180, 180)
        .pause(HINT_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem5()) // cut
        .click(pageObject.ymapsItem5())
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'road-drawn')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'road-long-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'road-long-on-map-selected');
}
