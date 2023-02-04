const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    LINE_NAME = 'линия_метро_автотест';

require('../common.js')(beforeEach, afterEach);

describe('metro line', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return drawMetroSectionLine.call(this.browser)
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'metro-line-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'metro-line-on-map-selected')

            .debugLog('Editing metro line...')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .then(() => linkMetroLine.call(this.browser, LINE_NAME))

            .debugLog('Changing attributes')
            .pointerClick(pageObject.control1() + ' ' + pageObject.radioButton1()) // (A) – tunnel
            .pointerClick(pageObject.control2() + ' ' + pageObject.radioButton5()) // (B) – bridge

            .debugLog('Changing geometry')
            .moveToObject('body', 200, 200)
            .pause(DRAW_TIMEOUT)
            .mouseDrag([200, 200], [230, 180], 500)
            .pause(DRAW_TIMEOUT)

            .debugLog('Cutting')
            .moveToObject('body', 230, 180)
            .pause(UPDATE_TIMEOUT)
            .pointerClick(230, 180)
            .pause(UPDATE_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem4()) // cut
            .click(pageObject.ymapsItem4())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'metro-line-changed-editor')
            .verifyMapScreenshot(100, 100, 200, 200, 'metro-line-changed-drawn')
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'metro-line-changed-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'metro-line-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'metro-line-changed-on-map-deselected');
    });

    it('is linked', function() {
        return drawMetroSectionLine.call(this.browser)
            .then(() => linkMetroLine.call(this.browser, LINE_NAME))
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'metro-line-linked-viewer')
            .closeSidebar();
    });
});

function drawMetroSectionLine() {
    return this
        .createGeoObject('transport_metro_el')
        .debugLog('Starting to draw a section of line...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'metro-line-drawn');
}

function linkMetroLine(name) {
    return this
        .debugLog('Link to metro line "' + name + '" by keys')
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), name)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .moveMouseAside()
        .waitForInvisible(pageObject.suggestPopup())
        .pause(UPDATE_TIMEOUT)
        .debugLog('New metro line selected from suggest');
}
