const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { COMMENTS_TIMEOUT, DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('cond-lane', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('can be created and changed correctly', function() {
        return createRoad.call(this.browser)
            .then(() => createRoadWithLanes.call(this.browser))
            .then(() => createCondLane.call(this.browser))
            .then(() => editCondLane.call(this.browser));
    });
});

function createRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 275)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .saveGeoObject();
}

function createRoadWithLanes() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(180, 180)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Cutting road')
        .pointerClick(180, 180)
        .pause(HINT_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem5()) // cut
        .click(pageObject.ymapsItem5())
        .debugLog('Geometry drawn')

        .debugLog('Setting lane to A')
        .debugLog('Adding default lane')
        .pointerClick(pageObject.section2() + ':nth-child(7) ' + pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.lanesEditorDirections())
        .pointerClick(pageObject.section2() + ':nth-child(7) ' + pageObject.lanesEditorActions() + ' ' + pageObject.button())

        .debugLog('Setting lanes to B')
        .debugLog('Adding tram lane')
        .pointerClick(pageObject.section2() + ':nth-child(6) ' + pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.lanesEditorDirections())
        .pointerClick(pageObject.l45Icon())
        .pointerClick(pageObject.l90Icon())
        .pointerClick(pageObject.l135Icon())
        .pointerClick(pageObject.l180Icon())
        .pointerClick(pageObject.slIcon())
        .pointerClick(pageObject.lrIcon())
        .pointerClick(pageObject.section2() + ':nth-child(6) ' + pageObject.lanesEditorActions() + ' ' + pageObject.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('editor', 'lane-kind-tram'))

        .debugLog('Adding bus lane')
        .pointerClick(pageObject.lanesEditorAddRight())
        .waitForVisible(pageObject.lanesEditorDirections())
        .pointerClick(pageObject.r45Icon())
        .pointerClick(pageObject.r90Icon())
        .pointerClick(pageObject.r135Icon())
        .pointerClick(pageObject.r180Icon())
        .pointerClick(pageObject.srIcon())
        .pointerClick(pageObject.rlIcon())
        .pointerClick(pageObject.section2() + ':nth-child(6) ' + pageObject.lanesEditorActions() + ' ' + pageObject.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('editor', 'lane-kind-bus'))

        .debugLog('Adding auto lane')
        .pointerClick(pageObject.lanesEditorAddLeft())
        .waitForVisible(pageObject.lanesEditorDirections())
        .pointerClick(pageObject.l180Icon())
        .pointerClick(pageObject.slIcon())
        .pointerClick(pageObject.lrIcon())
        .pointerClick(pageObject.r180Icon())
        .pointerClick(pageObject.srIcon())
        .pointerClick(pageObject.rlIcon())

        .debugLog('Adding bike lane')
        .pointerClick(pageObject.lanesEditorSelector.tab3())
        .pointerClick(pageObject.lanesEditorAddRight())
        .waitForVisible(pageObject.lanesEditorDirections())
        .pointerClick(pageObject.sIcon())
        .pointerClick(pageObject.r45Icon())
        .pointerClick(pageObject.r90Icon())
        .pointerClick(pageObject.r135Icon())
        .pointerClick(pageObject.r180Icon())
        .pointerClick(pageObject.srIcon())
        .pointerClick(pageObject.rlIcon())
        .pointerClick(pageObject.section2() + ':nth-child(6) ' + pageObject.lanesEditorActions() + ' ' + pageObject.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('editor', 'lane-kind-bike'))

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'road-to-b-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'road-to-b-history');
}

function createCondLane() {
    return this
        .pointerClick(250, 250)
        .waitForVisible(pageObject.geoObjRelsAddCondLane())
        .debugLog('Adding cond_lane')
        .pointerClick(pageObject.geoObjRelsAddCondLane())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Drawing cond_lane geometry...')
        .waitForNotification('suggest-cond-editor-set-from')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 200)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Setting cond_lane attributes')
        .pointerClick(pageObject.slIcon())
        .pause(UPDATE_TIMEOUT)
        .debugLog('Attributes set')
        .verifyMapScreenshot(100, 100, 200, 200, 'cond-lane-drawn')
        .saveGeoObject();
}

function editCondLane() {
    return this
        .pointerClick(250, 250)
        .waitForVisible(pageObject.geoObjRelsCondLane())
        .pointerClick(pageObject.geoObjRelsCondLane())
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pause(COMMENTS_TIMEOUT)

        .debugLog('Editing cond_lane')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing cond_lane geometry...')
        .pointerClick(pageObject.redrawIcon())
        .waitForNotification('suggest-editing-cond')
        .pointerClick(250, 250)
        .waitForNotification('suggest-cond-editor-set-from')
        .pointerClick(200, 200)
        .waitForNotification('suggest-cond-editor-set-to')
        .pointerClick(250, 270)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry changed')

        .debugLog('Changing cond_lane attributes...')
        .pointerClick(pageObject.rlIcon())
        .pause(UPDATE_TIMEOUT)
        .pointerClick(pageObject.condLaneEditorLane2())
        .pointerClick(pageObject.condLaneEditorLane3())
        .pointerClick(pageObject.bikeIcon())
        .pointerClick(pageObject.busIcon())
        .pointerClick(200, 200)
        .verifyScreenshot(pageObject.geoObjEditorView(), 'cond-lane-changed-editor')
        .pointerClick(pageObject.listCtrl.controlAdd())
        .waitForVisible(pageObject.timeRangeEdit())
        .debugLog('Attributes changed')
        .verifyMapScreenshot(100, 100, 200, 200, 'cond-lane-changed-drawn')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'cond-lane-changed-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'cond-lane-changed-history');
}
