const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('traffic-light', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('can be created and changed correctly', function() {
        return createCrossroad.call(this.browser)
            .then(() => createTrafficLight.call(this.browser))
            .then(() => editTrafficLight.call(this.browser));
    });
});

function createCrossroad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing horizontal road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 350)
        .pause(DRAW_TIMEOUT)
        .pointerClick(350, 350)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 200, 300, 300, 'road-horizontal-drawn')
        .saveGeoObject()

        .createGeoObject('rd')
        .debugLog('Drawing vertical road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 450)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 200, 300, 300, 'road-vertical-drawn')
        .saveGeoObject();
}

function createTrafficLight() {
    return this
        .createGeoObject('cond_traffic_light')
        .debugLog('Drawing traffic-light geometry...')
        .waitForNotification('suggest-creating-traffic-light')
        .moveToObject('body', 250, 350)
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'traffic-light-before-junction-selected')
        .pointerClick(250, 350)
        .waitForNotification('suggest-choosing-controlled-elements')
        .verifyMapScreenshot(100, 200, 300, 300, 'traffic-light-after-junction-selected')
        .pointerClick(250, 375)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 325)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 200, 300, 300, 'traffic-light-drawn')
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'traffic-light-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'traffic-light-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'traffic-light-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'traffic-light-on-map-selected');
}

function editTrafficLight() {
    return this
        .debugLog('Editing traffic-light...')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing traffic-light geometry...')
        .waitForNotification('suggest-choosing-controlled-elements')
        .pointerClick(275, 350)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 375)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry changed')
        .moveMouseAside()
        .verifyMapScreenshot(100, 200, 300, 300, 'traffic-light-changed-drawn')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'traffic-light-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'traffic-light-changed-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'traffic-light-changed-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'traffic-light-changed-on-map-selected')
        .closeSidebar()
        .verifyMapScreenshot(100, 200, 300, 300, 'traffic-light-changed-on-map-deselected');
}
