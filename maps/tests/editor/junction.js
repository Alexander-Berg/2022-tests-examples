const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('junction', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is created and moved', function() {
        return drawRoads.call(this.browser)
            .pointerClick(250, 350)
            .pause(DRAW_TIMEOUT)
            .waitForVisible(pageObject.geoObjViewerView())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-viewer')
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-on-map-selected')

            .debugLog('Editing the junction...')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'junction-editor-disabled')

            .debugLog('Moving the junction by click...')
            .pause(DRAW_TIMEOUT)
            .pointerClick(275, 375)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-moved-click-drawn')
            .debugLog('The junction is moved by click')

            .debugLog('Moving the junction by drag...')
            .pause(DRAW_TIMEOUT)
            .mouseDrag([275, 375], [200, 300])
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-moved-drag-drawn')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'junction-editor-active')
            .debugLog('The junction is moved by drag')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-moved-viewer')
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-moved-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-moved-on-map-deselected');
    });

    it('\'s roads are modified', function() {
        return drawRoads.call(this.browser)
            .pointerClick(250, 350)
            .pause(DRAW_TIMEOUT)
            .waitForVisible(pageObject.geoObjViewerView())
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())

            .debugLog('Editing the junction...')
            .waitForVisible(pageObject.editIcon())
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'junction-editor-disabled')

            .debugLog('Creating a new vertex...')
            .pause(DRAW_TIMEOUT)
            .mouseDrag([250, 300], [200, 300])
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-vertex-create-drawn')
            .debugLog('The vertex is created')

            .debugLog('Deleting an existing vertex...')
            .pause(DRAW_TIMEOUT)
            .leftClick('body', 250, 400)
            .leftClick('body', 250, 400)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-vertex-delete-drawn')
            .debugLog('The vertex is deleted')

            .debugLog('Continue drawing the road...')
            .pause(DRAW_TIMEOUT)
            .pointerClick(300, 400)
            .pause(HINT_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem0()) // continue drawing
            .click(pageObject.ymapsItem0())
            .pause(DRAW_TIMEOUT)
            .pointerClick(250, 400)
            .pause(DRAW_TIMEOUT)
            .pointerClick(200, 400)
            .pause(DRAW_TIMEOUT)
            .pointerClick(200, 400)
            .pause(HINT_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem1()) // finish drawing
            .click(pageObject.ymapsItem1())
            .moveMouseAside()
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-changed-drawn')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'junction-editor-active')
            .debugLog('The road is drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-changed-viewer')
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-changed-on-map-deselected');
    });

    it('has condition', function() {
        return drawRoads.call(this.browser)
            .pointerClick(250, 350)
            .pause(DRAW_TIMEOUT)
            .waitForVisible(pageObject.geoObjViewerView())

            .debugLog('Adding condition...')
            .waitForVisible(pageObject.junctionCondsViewAdd())
            .pointerClick(pageObject.junctionCondsViewAdd())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'condition-editor-disabled')

            .debugLog('Starting to draw a condition...')
            .pause(DRAW_TIMEOUT)
            .pointerClick(200, 350)
            .pause(DRAW_TIMEOUT)
            .pointerClick(300, 350)
            .moveMouseAside()
            .verifyMapScreenshot(100, 200, 300, 300, 'condition-drawn')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'condition-editor-active')
            .debugLog('The condition is drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-with-condition-viewer')
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-with-condition-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 200, 300, 300, 'junction-with-condition-on-map-deselected');
    });
});

function drawRoads() {
    return this
        .createGeoObject('rd')
        .debugLog('Starting to draw a road...')

        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 350)
        .pause(DRAW_TIMEOUT)
        .pointerClick(350, 350)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'road-linear-drawn')

        .saveGeoObject()

        .createGeoObject('rd')
        .debugLog('Starting to draw a crossing road...')

        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 400)
        .pause(DRAW_TIMEOUT)
        .pointerClick(300, 400)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'road-crossing-drawn')

        .saveGeoObject();
}
