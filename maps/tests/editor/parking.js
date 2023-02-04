const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('parking lot', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return createParkingLot.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'parking-lot-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-lot-on-map-selected')

            .debugLog('Editing parking lot')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'parking-lot-editor')

            .debugLog('Changing parking lot attributes...')
            .pointerClick(pageObject.nkCheckboxControl1() + ' ' + pageObject.nkCheckboxBox()) // uncheck toll
            .pointerClick(pageObject.nkCheckboxControl2() + ' ' + pageObject.nkCheckboxBox()) // uncheck in building
            .pointerClick(pageObject.nkCheckboxControl3() + ' ' + pageObject.nkCheckboxBox()) // check residental
            .pointerClick(pageObject.nkCheckboxControl4() + ' ' + pageObject.nkCheckboxBox()) // park&ride
            .pointerClick(pageObject.inputControl.input()) // number of places
            .setValue(pageObject.inputControl.input(), '123')
            .debugLog('Attributes changed')

            .debugLog('Changing parking lot geometry...')
            .waitForNotification('suggest-edit-point')
            .pointerClick(250, 250)
            .pause(DRAW_TIMEOUT)
            .debugLog('Geometry changed')
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-lot-changed-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'parking-lot-changed-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-lot-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-lot-changed-on-map-deselected');
    });

    it('is linked to parking territory', function() {
        return createParkingLot.call(this.browser)
            .createGeoObject('urban')
            .debugLog('Drawing parking territory geometry...')
            .waitForNotification('suggest-draw-geometry')
            .pointerClick(150, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 250)
            .pause(DRAW_TIMEOUT)
            .pointerClick(150, 250)
            .pause(DRAW_TIMEOUT)
            .debugLog('Geometry drawn')
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-territory-drawn')

            .debugLog('Setting parking territory attributes...')
            .debugLog('Linking parking territory to parking lot')
            .pointerClick(pageObject.geoObjMasterEditorViewSuggest.input())
            .waitForVisible(pageObject.suggestPopup())
            .pause(SUGGEST_TIMEOUT)
            .keys(['ArrowDown', 'Enter'])
            .waitForInvisible(pageObject.suggestPopup())
            .selectFtType('urban_areal-ft_type_id__251') // parking
            .debugLog('Attributes set')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'parking-territory-linked-editor')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'parking-territory-linked-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-territory-linked-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-territory-linked-on-map-deselected')

            .moveToObject('body', 180, 180)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'parking-territory-linked-hint');
    });
});

function createParkingLot() {
    return this
        .createGeoObject('urban_roadnet_parking_lot')
        .debugLog('Drawing parking geometry...')
        .waitForNotification('suggest-draw-point')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 100, 200, 200, 'parking-lot-drawn')

        .debugLog('Setting parking attributes...')
        .pointerClick(pageObject.nkCheckboxControl1() + ' ' + pageObject.nkCheckboxBox()) // check toll
        .pointerClick(pageObject.nkCheckboxControl2() + ' ' + pageObject.nkCheckboxBox()) // check in building
        .pointerClick(pageObject.inputControl.input()) // number of places
        .setValue(pageObject.inputControl.input(), '12')
        .debugLog('Attributes set')

        .saveGeoObject();
}
