const pageObject = require('../../page-object.js');
const { COMMENTS_TIMEOUT, DRAW_TIMEOUT, SUGGEST_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');
const arrivalPointAttrs = {
    name: 'точка_прибытия_автотест',
    streetName: 'Тестовая улица',
    addressNumber: '17',
    objLang: 'ru'
};

require('../common.js')(beforeEach, afterEach);

describe('arrival_point', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return createStreet.call(this.browser)
            .ensureLogoutFast()
            .debugLog('Loggin in as a cartographer')
            .ensureLoggedInFast('cartographer')
            .prepareNmaps()
            .then(() => createAddress.call(this.browser, arrivalPointAttrs.streetName))
            .then(() => createArrivalPoint.call(this.browser))
            .then(() => editArrivalPoint.call(this.browser, arrivalPointAttrs));
    });
});

function createStreet() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 125)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 125)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn...')

        .debugLog('Creating a street')
        .pointerClick(pageObject.nkGrid() + ':nth-child(3) ' + pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.nkGrid() + ':nth-child(3) ' + pageObject.geoObjMastersEditorViewSuggest.input(),
            arrivalPointAttrs.streetName)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())

        .saveGeoObject();
}

function createAddress(linkName) {
    return this
        .createGeoObject('addr')
        .debugLog('Putting address on map')
        .waitForNotification('suggest-draw-point')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)

        .debugLog('Linking to ' + linkName)
        .pointerClick(pageObject.geoObjEditorView.addLinkInput())
        .waitForVisible(pageObject.suggestPopup())
        .pointerClick(pageObject.suggestItem() + '=' + linkName)
        .waitForInvisible(pageObject.suggestPopup())

        .debugLog('Setting address name')
        .pointerClick(pageObject.group1() + ' ' + pageObject.nkTextInputControl())
        .setValue(pageObject.group1() + ' ' + pageObject.nkTextInputControl(), arrivalPointAttrs.addressNumber)
        .pointerClick(pageObject.moreIcon())
        .waitForVisible(pageObject.addrNameLang())
        .selectNameAttrs(arrivalPointAttrs.objLang)
        .waitForInvisible(pageObject.submitDisabled())

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'address-viewer-no-arrival-point');
}

function createArrivalPoint() {
    return this
        .debugLog('Adding arrival_point')
        .waitForVisible(pageObject.geoObjRelsAddArrivalPoint())
        .pointerClick(pageObject.geoObjRelsAddArrivalPoint())
        .debugLog('Putting arrival_point on map')
        .waitForNotification('suggest-draw-point')
        .pointerClick(150, 200)
        .pause(DRAW_TIMEOUT)
        .debugLog('Setting \'is_major\' attr')
        .pointerClick(pageObject.group2() + ' ' + pageObject.nkCheckboxControl2() + ' ' + pageObject.nkCheckbox())
        .verifyMapScreenshot(100, 135, 200, 165, 'arrival-point-default-drawn')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'address-viewer-with-arrival-point')
        .moveToObject(pageObject.geoObjRelsArrivalPoint())
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 135, 200, 165, 'arrival-point-on-map-pin')
        .closeSidebar()
        .verifyMapScreenshot(100, 135, 200, 165, 'arrival-point-default-on-map-deselected');
}

function editArrivalPoint(attrs) {
    return this
        .debugLog('Editing arrival_point')
        .pointerClick(200, 200)
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pointerClick(pageObject.geoObjRelsArrivalPoint())
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pause(COMMENTS_TIMEOUT)
        .verifyScreenshot(pageObject.geoObjViewerView(), 'arrival-point-default-viewer')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'arrival-point-default-editor')

        .debugLog('Setting arrival_point name as ' + attrs.name)
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.group1() + ' ' + pageObject.nkTextInputControl())
        .pointerClick(pageObject.group1() + ' ' + pageObject.nkTextInputControl())
        .setValue(pageObject.group1() + ' ' + pageObject.nkTextInputControl(), attrs.name)
        .selectNameAttrs(attrs.objLang)

        .debugLog('Changing arrival_point attributes')
        .pointerClick(pageObject.group2() + ' ' + pageObject.nkCheckboxControl1() + ' ' + pageObject.nkCheckbox())
        .pointerClick(pageObject.group2() + ' ' + pageObject.nkCheckboxControl2() + ' ' + pageObject.nkCheckbox())

        .debugLog('Changing arrival_point geometry')
        .waitForNotification('suggest-edit-point')
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 135, 200, 165, 'arrival-point-changed-drawn')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'arrival-point-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'arrival-point-changed-viewer')
        .verifyMapScreenshot(100, 135, 200, 165, 'arrival-point-changed-on-map-selected')

        .checkHistoryCommits('arrival-point-changed')
        .closeSidebar();
}
