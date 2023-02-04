const pageObject = require('../../page-object.js'),
    { COMMENTS_TIMEOUT, DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants'),
    coordsCamera = {
        center: [250, 350],
        isDirected: [250, 400],
        isNotDirected: [250, 300]
    };

require('../common.js')(beforeEach, afterEach);

describe('object', function() {
    beforeEach(function() {
        return this.browser.initNmaps('moderator');
    });

    it('is deleted by hotkeys', function() {
        return drawRoad.call(this.browser)
            .then(() => createCamera.call(this.browser))

            .debugLog('Deleting part of road...')
            .pointerClick(coordsCamera.isNotDirected[0], coordsCamera.isNotDirected[1])
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .pause(UPDATE_TIMEOUT)
            .keys(['Control', 'Delete'])
            .waitForVisible(pageObject.confirmationView.submit())
            .keys('Enter')
            .waitForInvisible(pageObject.geoObjViewerView())
            .waitForVisible(pageObject.notificationSuccess())
            .setMapCenterByTestNumber()
            .pause(DRAW_TIMEOUT)
            .moveMouseAside()
            .verifyMapScreenshot(100, 200, 300, 300, 'road-deleted-on-map-deselected')

            .debugLog('Verifying hint and card...')
            .moveToObject('body', coordsCamera.center[0], coordsCamera.center[1])
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 200, 300, 300, 'camera-on-map-hint')
            .pointerClick(coordsCamera.center[0], coordsCamera.center[1])
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .pause(COMMENTS_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-viewer')
            .moveToObject(pageObject.geoObjRelsViewItem.link())
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 200, 300, 300, 'camera-highlighted-on-hover');
    });
});

function createCamera() {
    return this
        .createGeoObject('cond_cam')
        .debugLog('Drawing camera geometry...')
        .waitForNotification('suggest-creating-cam')
        .pointerClick(coordsCamera.center[0], coordsCamera.center[1])
        .waitForNotification('suggest-choosing-cam-direction')
        .pointerClick(coordsCamera.isDirected[0], coordsCamera.isDirected[1])
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .moveMouseAside()
        .verifyMapScreenshot(100, 200, 300, 300, 'camera-drawn')
        .saveGeoObject();
}

function drawRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Starting to draw a road...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 450)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'road-drawn')

        .saveGeoObject();
}
