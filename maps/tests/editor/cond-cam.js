const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants'),
    condAttrsChanged = {
        startTime: '00:00',
        endTime: '23:59',
        startDate: '01.01',
        endDate: '31.12',
        ftType: 'cond_cam-cond_type__17' // dps
    };

require('../common.js')(beforeEach, afterEach);

describe('cond-cam', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('can be created and changed correctly', function() {
        return createRoad.call(this.browser)
            .then(() => createCondCam.call(this.browser))
            .then(() => editCondCam.call(this.browser, condAttrsChanged));
    });
});

function createRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 450)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 200, 300, 300, 'road-drawn')
        .saveGeoObject();
}

function createCondCam() {
    return this
        .createGeoObject('cond_cam')
        .debugLog('Drawing camera geometry...')
        .waitForNotification('suggest-creating-cam')
        .moveToObject('body', 250, 400)
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'camera-before-road-cut')
        .pointerClick(250, 400)
        .waitForNotification('suggest-choosing-cam-direction')
        .verifyMapScreenshot(100, 200, 300, 300, 'camera-after-road-cut')
        .pointerClick(250, 425)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .moveMouseAside()
        .verifyMapScreenshot(100, 200, 300, 300, 'camera-drawn')

        .debugLog('Setting camera default time range...')
        .pointerClick(pageObject.listCtrl.controlAdd())
        .waitForVisible(pageObject.timeRangeEdit())
        .pause(UPDATE_TIMEOUT)
        .debugLog('Time range set')
        .pointerClick(200, 200)
        .moveMouseAside()
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'camera-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'camera-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'camera-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'camera-on-map-selected');
}

function editCondCam(attrs) {
    return this
        .debugLog('Editing camera')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing camera geometry...')
        .waitForNotification('suggest-choosing-cam-direction')
        .pointerClick(250, 375)
        .pause(UPDATE_TIMEOUT)
        .debugLog('Geometry changed')
        .moveMouseAside()
        .verifyMapScreenshot(100, 200, 300, 300, 'camera-changed-drawn')

        .debugLog('Changing camera attributes...')
        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.fieldsetTimeStart())
        .setValue(pageObject.fieldsetTimeStart(), attrs.startTime)
        .pointerClick(pageObject.fieldsetTimeEnd())
        .setValue(pageObject.fieldsetTimeEnd(), attrs.endTime)
        .pointerClick(pageObject.fieldsetDateStart())
        .setValue(pageObject.fieldsetDateStart(), attrs.startDate)
        .pointerClick(pageObject.fieldsetDateEnd())
        .setValue(pageObject.fieldsetDateEnd(), attrs.endDate)
        .pointerClick(pageObject.allWorkingDays())
        .pointerClick(pageObject.allWeekendDays())
        .waitForVisible(pageObject.submitDisabled())
        .pointerClick(200, 200)
        .verifyScreenshot(pageObject.geoObjEditorView(), 'camera-null-editor')
        .pointerClick(pageObject.workingDay() + ':nth-child(3)') // wednesday
        .pointerClick(pageObject.weekendDay() + ':nth-child(2)') // sunday
        .debugLog('Attributes changed')
        .pointerClick(200, 200)
        .verifyScreenshot(pageObject.geoObjEditorView(), 'camera-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'camera-changed-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'camera-changed-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'camera-changed-on-map-selected')
        .closeSidebar()
        .verifyMapScreenshot(100, 200, 300, 300, 'camera-changed-on-map-deselected');
}
