const pageObject = require('../../page-object.js');
const i18n = require('../../tools/i18n.js');
const { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');
const sportTrack = {
        name: 'спорттрасса_автотест',
        ftType: 'sport_track-ft_type_id__2905' // dotted black ski trail
    };

require('../common.js')(beforeEach, afterEach);

describe('sport track', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('can be created and changed correctly', function() {
        return createRoad.call(this.browser)
            .then(() => createSportTrack.call(this.browser, sportTrack))
            .then(() => editSportTrack.call(this.browser, sportTrack))
    });
});

function createRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(350, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(HINT_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem5()) // cut
        .click(pageObject.ymapsItem5())
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 200, 300, 100, 'road-drawn')
        .saveGeoObject();
}

function createSportTrack(obj) {
    return this
        .createGeoObjectBySuggest('sport_track')
        .debugLog('Creating sport track...')
        .waitForNotification('suggest-select-slaves')
        .pointerClick(200, 250)
        .pause(DRAW_TIMEOUT)
        .moveMouseAside()
        .verifyMapScreenshot(100, 200, 300, 100, 'sport-track-drawn')

        .debugLog('Setting sport track name as ' + obj.name)
        .verifyScreenshot(pageObject.geoObjEditorView(), 'sport-track-default-editor')
        .pointerClick(pageObject.fieldsetCtrl1.input())
        .setValue(pageObject.fieldsetCtrl1.input(), obj.name)

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'sport-track-viewer');
}

function editSportTrack(obj) {
    return this
        .debugLog('Editing sport track...')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing sport track element and attrs')
        .pointerClick(200, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(300, 250)
        .pause(DRAW_TIMEOUT)
        .moveMouseAside()
        .verifyMapScreenshot(100, 200, 300, 100, 'sport-track-changed-drawn')
        .pointerClick(pageObject.group1() + ' ' + pageObject.select.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', obj.ftType))
        .verifyScreenshot(pageObject.geoObjEditorView(), 'sport-track-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'sport-track-changed-viewer')
        .verifyMapScreenshot(100, 200, 300, 100, 'sport-track-on-map-selected')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'sport-track-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .closeSidebar()
        .verifyMapScreenshot(100, 200, 300, 100, 'sport-track-on-map-deselected');
}
