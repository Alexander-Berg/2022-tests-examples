const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    STATION_NAME = 'жд_станция_тест',
    PLATFORM_NAME = 'платформа_тест';

require('../common.js')(beforeEach, afterEach);

describe('platform', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return drawRailroadStation.call(this.browser)
            .createGeoObject('transport_railway_platform')
            .debugLog('Starting to draw a platform...')

            .pause(DRAW_TIMEOUT)
            .pointerClick(150, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 250)
            .pause(DRAW_TIMEOUT)
            .pointerClick(150, 250)
            .pause(DRAW_TIMEOUT)

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'platform-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'platform-on-map-selected')

            .debugLog('Editing the platform...')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'platform-editor')

            .debugLog('Changing platform attributes')
            .pointerClick(pageObject.geoObjMasterEditorViewSuggest.input())
            .waitForVisible(pageObject.suggestPopup())
            .pause(SUGGEST_TIMEOUT)
            .keys(['ArrowDown', 'Enter'])
            .waitForInvisible(pageObject.suggestPopup())
            .pointerClick(pageObject.nkLinkViewAction())
            .waitForVisible(pageObject.textInputControl())
            .pointerClick(pageObject.textInputControl())
            .setValue(pageObject.textInputControl(), PLATFORM_NAME)
            .selectNameAttrs()

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'platform-changed-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'platform-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'platform-changed-on-map-deselected')

            .moveToObject('body', 180, 180)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 100, 350, 200, 'platform-changed-hint');
    });
});

function drawRailroadStation() {
    return this
        .createGeoObject('transport_railway_station')
        .debugLog('Starting to draw a railroad station...')

        .pause(DRAW_TIMEOUT)
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'railroad-station-drawn')

        .debugLog('Setting station name...')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.listCtrl() + ' ' + pageObject.nkTextInputControl())
        .waitForVisible(pageObject.textInputFocused())
        .setValue(pageObject.listCtrl() + ' ' + pageObject.nkTextInputControl(), STATION_NAME)
        .selectNameAttrs()

        .saveGeoObject();
}
