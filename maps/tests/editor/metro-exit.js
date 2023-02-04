const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    STATION_NAME = 'станция_метро_автотест',
    EXIT_NAME = 'выход_метро_автотест';

require('../common.js')(beforeEach, afterEach);

describe('metro exit', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return drawMetroStation.call(this.browser, STATION_NAME)
            .then(() => drawMetroExit.call(this.browser))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'metro-exit-viewer')
            .verifyMapScreenshot(260, 260, 100, 100, 'metro-exit-on-map-selected')

            .debugLog('Editing metro exit...')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .then(() => linkMetroExit.call(this.browser))

            .debugLog('Add exit name')
            .pointerClick(pageObject.nkLinkViewAction())
            .waitForVisible(pageObject.textInputControl())
            .pointerClick(pageObject.textInputControl())
            .setValue(pageObject.textInputControl(), EXIT_NAME)

            .debugLog('Changing geometry')
            .pointerClick(220, 220)
            .pause(DRAW_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjEditorView(), 'metro-exit-changed-editor')
            .verifyMapScreenshot(120, 120, 250, 250, 'metro-exit-changed-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'metro-exit-changed-viewer')
            .verifyMapScreenshot(100, 100, 200, 125, 'metro-exit-changed-on-map-selected')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 125, 'metro-exit-changed-on-map-deselected');
    });
});

function drawMetroStation(name) {
    return this.createGeoObject('transport_metro_station')
        .debugLog('Starting to draw a station...')
        .waitForNotification('suggest-draw-point')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)

        .debugLog('Add station name')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), name)
        .selectNameAttrs('ru')

        .saveGeoObject();
}

function drawMetroExit() {
    return this.createGeoObject('transport_metro_exit')
        .debugLog('Starting to draw a station...')
        .waitForNotification('suggest-draw-point')
        .pointerClick(310, 310)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(260, 260, 100, 100, 'metro-exit-drawn')

        .saveGeoObject();
}

function linkMetroExit() {
    return this
        .debugLog('Link exit to exist metro station')
        .pointerClick(pageObject.geoObjMasterEditorViewSuggest.input())
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter']) // choose exist station
        .debugLog('Metro line selected from suggest');
}
