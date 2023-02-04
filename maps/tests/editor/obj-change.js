const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('object by common', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('can be changed by cartographer and has last commit approved', function() {
        return createRoad.call(this.browser)
            .then(() => createTrafficLight.call(this.browser))
            .ensureLogoutFast()
            .debugLog('Log in as a cartographer')
            .ensureLoggedInFast('cartographer')
            .prepareNmaps()
            .then(() => editTrafficLight.call(this.browser));
    });
});

function createRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .saveGeoObject();
}

function createTrafficLight() {
    return this
        .createGeoObject('cond_traffic_light')
        .debugLog('Drawing traffic-light geometry...')
        .waitForNotification('suggest-creating-traffic-light')
        .pointerClick(210, 210)
        .waitForNotification('suggest-choosing-controlled-elements')
        .pointerClick(175, 175)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .waitForInvisible(pageObject.submitDisabled())
        .saveGeoObject();
}

function editTrafficLight() {
    return this
        .pointerClick(210, 210)
        .waitForVisible(pageObject.geoObjViewerView())
        .waitForVisible(pageObject.geoObjRelsViewItem.link())
        .pointerClick(pageObject.geoObjRelsViewItem.link())
        .waitForVisible(pageObject.geoObjViewerView())
        .waitForVisible(pageObject.editIcon())
        .debugLog('Editing traffic-light...')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing traffic-light geometry...')
        .waitForNotification('suggest-choosing-controlled-elements')
        .pointerClick(225, 225)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry changed')
        .moveMouseAside()
        .verifyMapScreenshot(100, 100, 200, 200, 'traffic-light-changed-drawn')
        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'traffic-light-changed-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'traffic-light-changed-history',
            { ignore: 'commit-history-date' });
}
