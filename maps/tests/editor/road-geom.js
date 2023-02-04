const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { COMMENTS_TIMEOUT, DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('road', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed by drag', function() {
        return drawRoad.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'road-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'road-on-map-selected')

            .debugLog('Editing road...')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing road geometry')
            .waitForNotification('suggest-edit-geometry')
            .moveToObject('body', 250, 250)
            .mouseDrag([250, 250], [225, 225])
            .pause(UPDATE_TIMEOUT)
            .moveMouseAside()
            .verifyMapScreenshot(100, 100, 200, 200, 'changed-road')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'changed-road-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'changed-road-on-map-deselected');
    });

    it('is re-drawn', function() {
        return drawRoad.call(this.browser)
            .debugLog('Editing road')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing road geometry')
            .pointerClick(pageObject.redrawIcon())
            .waitForNotification('suggest-edit-geometry')
            .pointerClick(250, 250)
            .pause(DRAW_TIMEOUT)
            .pointerClick(150, 250)
            .pause(DRAW_TIMEOUT)
            .pointerClick(150, 150)
            .pause(DRAW_TIMEOUT)
            .debugLog('Undo by button click')
            .pointerClick(pageObject.undoIcon())
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'redrawn-road-undo')
            .pause(UPDATE_TIMEOUT)
            .debugLog('Redo by hotkey')
            .keyPress(['Control', 'y'])
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'redrawn-road-redo')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'redrawn-road-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'redrawn-road-on-map-deselected');
    });

    it('is rounded', function() {
        return drawRoad.call(this.browser)
            .debugLog('Editing road...')
            .keyPress(['Control', 'e'])
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing road geometry')
            .waitForNotification('suggest-edit-geometry')
            .pointerClick(250, 150)
            .pause(HINT_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem3()) // round angle
            .click(pageObject.ymapsItem3())
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'rounded-road')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'rounded-road-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'rounded-road-on-map-deselected');
    });

    it('is cut', function() {
        return drawAndCutRoad.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'road-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'cut-road-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'cut-road-on-map-deselected');
    });

    it('\'s junction is deleted', function() {
        return drawAndCutRoad.call(this.browser)
            .pointerClick(250, 150)
            .waitForVisible(pageObject.geoObjViewerView())
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .pause(COMMENTS_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-viewer')

            .debugLog('Deleting junction...')
            .pointerClick(pageObject.moreIcon())
            .waitForVisible(pageObject.nkPopup())
            .selectElementByTextInMenu(i18n('common', 'delete'))
            .waitForVisible(pageObject.confirmationView.submit())
            .pointerClick(pageObject.confirmationView.submit())
            .waitForVisible(pageObject.notificationSuccess())
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'road-with-no-junction-on-map-deselected');
    });
});

function drawRoad() {
    return this
        .createGeoObject('rd')

        .debugLog('Starting to draw a road...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'road-drawn')

        .saveGeoObject();
}

function drawAndCutRoad() {
    return this
        .createGeoObject('rd')

        .debugLog('Starting to draw a road...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(160, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(UPDATE_TIMEOUT)
        .pointerClick(250, 150)
        .pause(UPDATE_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem5()) // cut
        .click(pageObject.ymapsItem5())
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'cut-road-drawn')

        .saveGeoObject();
}
