const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    parking = {
        ftType: 'urban_areal-ft_type_id__251',
        name: 'зона_тест'
    };

require('../common.js')(beforeEach, afterEach);

describe('parking zone', function() { // TODO create user with privileges for parking zones
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('is drawn and linked with parking linear', function() {
        return drawParkingZone.call(this.browser, parking)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'parking-zone-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-zone-on-map-selected')

            .createGeoObject('urban_roadnet_parking_lot_linear')
            .debugLog('Starting to draw a linear parking...')
            .pause(DRAW_TIMEOUT)
            .pointerClick(150, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 150)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-drawn')

            .debugLog('Linking linear parking to parking zone...')
            .click(pageObject.geoObjMasterEditorViewSuggest.input())
            .waitForVisible(pageObject.suggestPopup())
            .pause(SUGGEST_TIMEOUT)
            .keys(['ArrowDown', 'Enter'])
            .waitForInvisible(pageObject.suggestPopup())

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'parking-linear-linked-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-linear-linked-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-linear-linked-on-map-deselected');
    });

    it('is linked with parking lot', function() {
        return drawParkingZone.call(this.browser, parking)
            .createGeoObject('urban_roadnet_parking_lot')
            .debugLog('Starting to draw a parking lot...')

            .pause(DRAW_TIMEOUT)
            .pointerClick(170, 170)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-lot-drawn')

            .debugLog('Linking parking lot to parking zone...')
            .click(pageObject.geoObjMasterEditorViewSuggest.input())
            .waitForVisible(pageObject.suggestPopup())
            .pause(SUGGEST_TIMEOUT)
            .keys(['ArrowDown', 'Enter'])
            .waitForInvisible(pageObject.suggestPopup())

            .saveGeoObject()

            .createGeoObject('urban')
            .debugLog('Starting to draw a parking territory...')

            .pause(DRAW_TIMEOUT)
            .pointerClick(150, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 250)
            .pause(DRAW_TIMEOUT)
            .pointerClick(150, 250)
            .pause(DRAW_TIMEOUT)

            .debugLog('Linking parking territory to parking lot...')
            .click(pageObject.geoObjMasterEditorViewSuggest.input())
            .waitForVisible(pageObject.suggestPopup())
            .pause(SUGGEST_TIMEOUT)
            .keys(['ArrowDown', 'Enter'])
            .waitForInvisible(pageObject.suggestPopup())

            .debugLog('Changing ft_type...')
            .selectFtType(parking.ftType)

            .pause(UPDATE_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjEditorView(), 'parking-territory-linked-editor')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'parking-territory-linked-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-territory-linked-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-territory-linked-on-map-deselected')

            .moveToObject('body', 170, 170)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 100, 400, 200, 'parking-lot-linked-hint');
    });
});

function drawParkingZone(attrs) {
    return this
        .createGeoObject('urban_roadnet_parking_controlled_zone')
        .debugLog('Starting to draw a parking zone...')

        .pause(DRAW_TIMEOUT)
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'parking-zone-drawn')

        .debugLog('Setting zone name...')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pause(UPDATE_TIMEOUT)
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs()

        .saveGeoObject();
}
