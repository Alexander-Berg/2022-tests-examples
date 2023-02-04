const pageObject = require('../../page-object.js'),
    { COMMENTS_TIMEOUT, DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('cond-toll', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('can be created and changed correctly', function() {
        return createRoad.call(this.browser)
            .then(() => createCondToll.call(this.browser))
            .then(() => editCondToll.call(this.browser));
    });
});

function createRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 200)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 200)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 100, 200, 200, 'road-drawn')
        .saveGeoObject();
}

function createCondToll() {
    return this
        .createGeoObject('cond_toll')
        .debugLog('Drawing toll geometry...')
        .waitForNotification('suggest-creating-toll-point')
        .moveToObject('body', 200, 200)
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'toll-before-road-cut')
        .pointerClick(200, 200)
        .waitForNotification('suggest-choosing-toll-point-direction')
        .verifyMapScreenshot(100, 100, 200, 200, 'toll-after-road-cut')
        .pointerClick(225, 200)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .moveMouseAside()
        .verifyMapScreenshot(100, 100, 200, 200, 'toll-drawn')
        .pointerClick(100, 100)
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'toll-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'toll-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'toll-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'toll-on-map-selected')
        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'toll-on-map-deselected');
}

function editCondToll() {
    return this
        .pointerClick(200, 200)
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pause(COMMENTS_TIMEOUT)
        .waitForVisible(pageObject.geoObjRelsCondToll())
        .pointerClick(pageObject.geoObjRelsCondToll())
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pause(COMMENTS_TIMEOUT)

        .debugLog('Editing toll')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing toll geometry...')
        .waitForNotification('suggest-choosing-toll-point-direction')
        .pointerClick(175, 200)
        .pause(UPDATE_TIMEOUT)
        .debugLog('Geometry changed')
        .moveMouseAside()
        .verifyMapScreenshot(100, 100, 200, 200, 'toll-changed-drawn')
        .pointerClick(100, 100)
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'toll-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'toll-changed-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'toll-changed-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'toll-changed-on-map-selected')

        .debugLog('Verifying highlight on hover')
        .pointerClick(200, 200)
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pause(COMMENTS_TIMEOUT)
        .waitForVisible(pageObject.geoObjRelsCondToll())
        .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-viewer')
        .moveToObject(pageObject.geoObjRelsViewItem.link())
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'toll-changed-on-map-hover');
}
