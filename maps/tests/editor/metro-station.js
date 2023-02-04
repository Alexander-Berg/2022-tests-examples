const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    station = {
        name: 'станция_метро_автотест'
    },
    line = {
        name: 'линия_метро_автотест'
    };

require('../common.js')(beforeEach, afterEach);

describe('metro station', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return drawAndLinkMetroLine.call(this.browser, line)
            .then(() => drawMetroStation.call(this.browser))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'metro-station-viewer')
            .verifyMapScreenshot(260, 260, 100, 100, 'metro-station-on-map-selected')

            .debugLog('Editing metro station...')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .then(() => linkMetroStation.call(this.browser))

            .debugLog('Add station name')
            .pointerClick(pageObject.nkLinkViewAction())
            .waitForVisible(pageObject.textInputControl())
            .pointerClick(pageObject.textInputControl())
            .setValue(pageObject.textInputControl(), station.name)
            .selectNameAttrs()

            .debugLog('Changing geometry')
            .pointerClick(200, 200)
            .pause(DRAW_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjEditorView(), 'metro-station-changed-editor')
            .verifyMapScreenshot(120, 120, 250, 250, 'metro-station-changed-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'metro-station-changed-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'metro-station-changed-on-map-selected')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'metro-station-changed-on-map-deselected');
    });
});

function drawAndLinkMetroLine(attrs) {
    return this
        .createGeoObject('transport_metro_el')
        .debugLog('Starting to draw a section of line...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'metro-line-drawn')

        .debugLog('Link to metro line ' + attrs.name)
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), attrs.name)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .pause(UPDATE_TIMEOUT)
        .debugLog('New metro line selected from suggest')

        .saveGeoObject();
}

function drawMetroStation() {
    return this.createGeoObject('transport_metro_station')
        .debugLog('Starting to draw a station...')
        .waitForNotification('suggest-draw-point')
        .pointerClick(310, 310)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(260, 260, 100, 100, 'metro-station-drawn')

        .saveGeoObject();
}

function linkMetroStation() {
    return this
        .debugLog('Link station to exist metro line')
        .pointerClick(pageObject.geoObjMasterEditorViewSuggest.input())
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter']) // choose exist line
        .debugLog('Metro line selected from suggest')
        .pause(UPDATE_TIMEOUT);
}
