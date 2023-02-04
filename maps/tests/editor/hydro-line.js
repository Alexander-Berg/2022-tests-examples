const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    HYDRO_NAME = 'река_автотест';

require('../common.js')(beforeEach, afterEach);

describe('hydro-line', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn', function() {
        return drawRiver.call(this.browser)
            .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-drawn')
            .saveGeoObject()
            .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-on-map-deselected');
    });

    it('is linked and changed', function() {
        return drawRiver.call(this.browser)
            .saveGeoObject()
            .closeSidebar()
            .then(() => drawSecondRiver.call(this.browser))
            .then(() => linkRiver.call(this.browser, HYDRO_NAME))
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'hydro-line-linked-viewer')
            .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-linked-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-linked-on-map-deselected')
            .then(() => editRiver.call(this.browser));
    });
});

function drawRiver() {
    return this
        .createGeoObject('hydro_ln_el')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 325)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 325)
        .pause(DRAW_TIMEOUT)
        .pointerClick(pageObject.roundAngles())
        .pause(UPDATE_TIMEOUT);
}

function drawSecondRiver() {
    return this
        .createGeoObject('hydro_ln_el')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .moveToObject('body', 150, 325)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-linked-snap')
        .pointerClick(150, 350)
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-linked-drawn');
}

function linkRiver(name) {
    return this
        .debugLog('Linking to ' + name)
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), name)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup());
}

function editRiver() {
    return this
        .debugLog('Editing river')
        .pointerClick(150, 275)
        .waitForVisible(pageObject.geoObjViewerView())
        .pointerClick(pageObject.geoObjRelsViewItem.link())
        .waitForVisible(pageObject.geoObjViewerView())
        .waitForVisible(pageObject.editIcon())
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing river elements...')
        .waitForNotification('suggest-select-slaves')
        .moveToObject('body', 150, 275)
        .pause(HINT_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-changed-hover-delete')
        .pointerClick(150, 275)
        .pause(DRAW_TIMEOUT)
        .moveToObject('body', 250, 275)
        .pause(HINT_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-changed-hover-add')
        .pointerClick(250, 275)
        .pause(DRAW_TIMEOUT)
        .debugLog('River elements changed')
        .pointerClick(300, 300)
        .pause(HINT_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-changed-drawn')

        .saveGeoObject()
        .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-changed-on-map-selected')

        .waitForVisible(pageObject.geoObjViewerView.close(), 450)
        .pointerClick(pageObject.geoObjViewerView.close())
        .pause(UPDATE_TIMEOUT)
        .closeSidebar()
        .verifyMapScreenshot(100, 200, 300, 200, 'hydro-line-changed-on-map-deselected');
}
