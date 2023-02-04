const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT, WAIT_FOR_CLASS_TIMEOUT } =
        require('../../tools/constants'),
    cond = {
        startTime: '00:00',
        endTime: '23:59',
        startDate: '01.01',
        endDate: '31.12',
        ftType: 'cond-cond_type__1', // prohibited manoeuvre
        wednesday: ':nth-child(3)',
        sunday: ':nth-child(2)'
    };

require('../common.js')(beforeEach, afterEach);

describe('cond', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('can be created and changed correctly', function() {
        return createRoad.call(this.browser)
            .then(() => createCond.call(this.browser))
            .then(() => editCond.call(this.browser, cond));
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

function createCond() {
    return this
        .createGeoObject('cond/cond_type__5-access_id__28')
        .debugLog('Drawing cond geometry...')
        .waitForNotification('suggest-creating-cond')
        .moveToObject('body', 250, 350)
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'cond-before-road-cut')
        .pointerClick(250, 350)
        .waitForNotification('suggest-cond-editor-set-from')
        .verifyMapScreenshot(100, 200, 300, 300, 'cond-after-road-cut')
        .pointerClick(250, 300)
        .waitForNotification('suggest-cond-editor-set-to')
        .pointerClick(250, 400)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .moveMouseAside()
        .verifyMapScreenshot(100, 200, 300, 300, 'cond-drawn')
        .pointerClick(100, 100)
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'cond-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'cond-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'cond-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'cond-on-map-selected');
}

function editCond(attrs) {
    return this
        .debugLog('Editing cond')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing cond geometry...')
        .pointerClick(pageObject.redrawIcon())
        .waitForNotification('suggest-editing-cond')
        .pointerClick(250, 350)
        .waitForNotification('suggest-cond-editor-set-from')
        .pointerClick(250, 400)
        .waitForNotification('suggest-cond-editor-set-to')
        .pointerClick(250, 300)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry changed')

        .debugLog('Changing cond attributes...')
        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.listCtrl.controlAdd())
        .waitForVisible(pageObject.timeRangeEdit())
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'cond-all-checked-editor')
        .pointerClick(pageObject.allWorkingDays())
        .pointerClick(pageObject.allWeekendDays())
        .waitForExist(pageObject.submitDisabled(), WAIT_FOR_CLASS_TIMEOUT)
        .pointerClick(pageObject.workingDay() + attrs.wednesday)
        .pointerClick(pageObject.weekendDay() + attrs.sunday)
        .waitForInvisible(pageObject.submitDisabled(), WAIT_FOR_CLASS_TIMEOUT)
        .pointerClick(pageObject.fieldsetTimeStart())
        .setValue(pageObject.fieldsetTimeStart(), attrs.startTime)
        .waitForExist(pageObject.submitDisabled(), WAIT_FOR_CLASS_TIMEOUT)
        .pointerClick(pageObject.fieldsetTimeEnd())
        .setValue(pageObject.fieldsetTimeEnd(), attrs.endTime)
        .waitForInvisible(pageObject.submitDisabled(), WAIT_FOR_CLASS_TIMEOUT)
        .pointerClick(pageObject.fieldsetDateStart())
        .setValue(pageObject.fieldsetDateStart(), attrs.startDate)
        .waitForExist(pageObject.submitDisabled(), WAIT_FOR_CLASS_TIMEOUT)
        .pointerClick(pageObject.fieldsetDateEnd())
        .setValue(pageObject.fieldsetDateEnd(), attrs.endDate)
        .waitForInvisible(pageObject.submitDisabled(), WAIT_FOR_CLASS_TIMEOUT)
        .debugLog('Changing types of vehicle')
        .pointerClick(pageObject.pedestrianIcon())
        .pointerClick(pageObject.bikeIcon())
        .pointerClick(pageObject.truckIcon())
        .debugLog('Attributes changed')
        .pointerClick(150, 150)
        .verifyMapScreenshot(100, 200, 300, 300, 'cond-changed-drawn')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'cond-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'cond-changed-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'cond-changed-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'cond-changed-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 200, 300, 300, 'cond-changed-on-map-deselected')

        .debugLog('Verifying hint and highlight on hover')
        .moveToObject('body', 250, 350)
        .pause(HINT_TIMEOUT)
        .verifyMapScreenshot(150, 200, 300, 300, 'cond-on-map-hint')
        .pointerClick(250, 350)
        .waitForVisible(pageObject.geoObjViewerView())
        .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-viewer')
        .verifyMapScreenshot(100, 200, 300, 300, 'cond-non-highlighted')
        .moveToObject(pageObject.junctionCondsViewItem.link())
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 200, 300, 300, 'cond-highlighted-on-hover');
}
