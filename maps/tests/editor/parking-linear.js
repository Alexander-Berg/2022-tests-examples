const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('linear parking', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return drawLinearParking.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'linear-parking-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-on-map-selected')

            .debugLog('Editing linear parking...')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'linear-parking-editor')

            .debugLog('Changing linear parking ft-type')
            .pointerClick(pageObject.select.button()) // ft_type
            .waitForVisible(pageObject.nkPopup.menuFocused())
            .selectElementByTextInMenu(i18n('attr-values', 'urban_roadnet_parking_lot_linear-ft_type_id__2003')) // limited

            .debugLog('Changing linear parking geometry...')
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 150)
            .pause(DRAW_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem1()) // continue drawing
            .click(pageObject.ymapsItem1())
            .pause(DRAW_TIMEOUT)
            .pointerClick(150, 250)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-changed-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'linear-parking-changed-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-changed-on-map-deselected');
    });

    it('is snapped', function() {
        return drawLinearParking.call(this.browser)
            .createGeoObject('urban_roadnet_parking_lot_linear')
            .debugLog('Starting to draw another linear parking...')

            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 250)
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', 250, 150)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-snapped')
            .pointerClick(250, 150)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-snapped-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'linear-parking-snapped-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-snapped-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-snapped-on-map-deselected');
    });
});

function drawLinearParking() {
    return this
        .createGeoObject('urban_roadnet_parking_lot_linear')
        .debugLog('Starting to draw a linear parking...')

        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'linear-parking-drawn')

        .saveGeoObject();
}
