const pageObject = require('../../page-object.js');
const { DRAW_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants');
const FIRST_STREET_NAME = 'улица1_автотест';
const SECOND_STREET_NAME = 'улица2_автотест';

require('../common.js')(beforeEach, afterEach);

describe('road', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is linked to the existing street', function() {
        return linkRoadToStreet.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'existing-road-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'existing-road-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'existing-road-on-map-deselected');
    });

    it('is linked to a new street', function() {
        return linkRoadToStreet.call(this.browser)

            .debugLog('Editing road')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'existing-road-editor')

            .debugLog('Changing relation')
            .pointerClick(pageObject.deleteIconSmall())
            .waitForVisible(pageObject.geoObjMastersEditorViewSuggest.input())
            .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
            .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), SECOND_STREET_NAME)
            .waitForVisible(pageObject.suggestPopup())
            .pause(SUGGEST_TIMEOUT)
            .keys(['ArrowDown', 'Enter'])
            .waitForInvisible(pageObject.suggestPopup())

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'new-road-viewer')
            .verifyMapScreenshot(100, 100, 200, 100, 'new-road-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 100, 'new-road-on-map-deselected', { tolerance: 75 });
    });
});

function linkRoadToStreet() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 100, 200, 200, 'first-road-drawn')

        .debugLog('Creating street')
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), FIRST_STREET_NAME)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'first-road-editor')

        .saveGeoObject()

        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 100, 200, 100, 'second-road-drawn')

        .debugLog('Linking street')
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'second-road-editor')

        .saveGeoObject();
}
