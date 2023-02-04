const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    waterwayStop = {
        name: 'пристань_автотест',
        nameLang: 'en',
        linkTo: 'линия1_автотест',
        coords: [150, 150]
    },
    waterwayStopChanged = {
        name: 'порт_автотест',
        nameLang: 'uk',
        ftType: 'transport_waterway_stop-ft_type_id__603',
        linkTo: 'линия2_автотест',
        coords: [200, 200]
    };

require('../common.js')(beforeEach, afterEach);

describe('waterway stop', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return createWaterwayStop.call(this.browser, waterwayStop)
            .then(() => editWaterwayStop.call(this.browser, waterwayStopChanged));
    });
});

function createWaterwayStop(obj) {
    return this
        .createGeoObject('transport_waterway_stop')
        .debugLog('Putting waterway stop on map')
        .waitForNotification('suggest-draw-point')
        .pointerClick(obj.coords[0], obj.coords[1])
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 100, 100, 'waterway-stop-drawn')

        .debugLog('Setting waterway stop name as ' + obj.name)
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.listCtrl() + ' ' + pageObject.nkTextInputControl())
        .setValue(pageObject.listCtrl() + ' ' + pageObject.nkTextInputControl(), obj.name)
        .selectNameAttrs(obj.nameLang)

        .debugLog('Linking to waterline ' + obj.linkTo)
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), obj.linkTo)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'waterway-stop-viewer')
        .verifyMapScreenshot(80, 100, 200, 150, 'waterway-stop-on-map-selected');
}

function editWaterwayStop(obj) {
    return this
        .debugLog('Editing waterway stop')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing waterway stop geometry')
        .waitForNotification('suggest-edit-point')
        .pointerClick(obj.coords[0], obj.coords[1])
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(80, 100, 200, 150, 'waterway-stop-changed-drawn')

        .debugLog('Changing waterway stop ft_type')
        .selectFtType(obj.ftType)

        .debugLog('Adding second name as ' + obj.name)
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.listCtrlItem() + ':nth-child(3) ' + pageObject.nkTextInputControl())
        .blurInput(pageObject.listCtrlItem() + ':nth-child(3) ' + pageObject.input())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'waterway-stop-changed-editor-2nd-name')
        .pointerClick(pageObject.listCtrlItem() + ':nth-child(3) ' + pageObject.nkTextInputControl())
        .setValue(pageObject.listCtrlItem() + ':nth-child(3) ' + pageObject.nkTextInputControl(), obj.name)
        .selectNameAttrs(obj.nameLang, null, 2)

        .debugLog('Linking to second waterline ' + obj.linkTo)
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), obj.linkTo)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .pause(UPDATE_TIMEOUT)
        .blurInput(pageObject.geoObjMastersEditorViewSuggest.input())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'waterway-stop-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'waterway-stop-changed-viewer')

        .debugLog('Checking waterline')
        .pointerClick(pageObject.transportRoutesViewerItem())
        .waitForVisible(pageObject.geoObjViewerView())
        .verifyScreenshot(pageObject.geoObjViewerView(), 'waterway-line-viewer')
        .setMapCenterByTestNumber()
        .moveToObject('body', obj.coords[0], obj.coords[1])
        .pause(HINT_TIMEOUT)
        .verifyMapScreenshot(125, 150, 400, 100, 'waterway-stop-on-map-hint')
        .checkFirstCommit('waterway-line');
}
