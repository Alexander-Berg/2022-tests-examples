const pageObject = require('../../page-object.js');
const i18n = require('../../tools/i18n.js');
const { DRAW_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants');
const addressName = '3';
const vegetationName = 'растительность_автотест';
const streetName = 'улица_автотест';
const lang = 'ru';

require('../common.js')(beforeEach, afterEach);

describe('address', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('not assigned can\'t be saved', function() {
        return drawAddress.call(this.browser)
            .verifyScreenshot(pageObject.geoObjEditorView(), 'address-not-assigned-editor')
            .pointerClick(pageObject.geoObjEditorView.submit())
            .waitForVisible(pageObject.notificationError())
            .pause(UPDATE_TIMEOUT)
            .verifyScreenshot(pageObject.notificationError(), 'address-not-assigned-error')
            .verifyMapScreenshot(100, 100, 200, 200, 'address-not-assigned-on-map-drawn');
    });

    it('changes assignment', function() {
        return createVegetation.call(this.browser)
            .then(() => createStreet.call(this.browser))
            .then(() => drawAddress.call(this.browser, vegetationName, 'master-group-other'))
            .verifyScreenshot(pageObject.geoObjEditorView(), 'address-assigned-to-vegetation-editor')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'address-assigned-to-vegetation-viewer',
                { ignore: 'viewer-date' })

            .then(() => editAddress.call(this.browser, streetName));
    });

    it('changes geometry', function() {
        return createBuilding.call(this.browser)
            .then(() => createStreet.call(this.browser))
            .then(() => drawAddress.call(this.browser, streetName))
            .saveGeoObject()
            .closeSidebar()

            .debugLog('Editing address')
            .pointerClick(190, 190)
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'building-with-address-viewer')
            .pointerClick(200, 200)
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing address geometry')
            .waitForNotification('suggest-edit-point')
            .pointerClick(200, 275)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 140, 200, 160, 'address-outside-drawn-1')
            .keys(['Control', 'z'])
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 140, 200, 160, 'address-inside-drawn')
            .pointerClick(pageObject.redoIcon())
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 140, 200, 160, 'address-outside-drawn-2')

            .saveGeoObject()
            .verifyMapScreenshot(100, 140, 200, 160, 'address-outside-on-map-selected')

            .pointerClick(190, 190)
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'building-with-no-address-viewer')

            .closeSidebar()
            .verifyMapScreenshot(100, 140, 200, 160, 'address-outside-on-map-deselected');
    });
});

function drawAddress(linkName, role) {
    return this
        .createGeoObject('addr')
        .debugLog('Putting address on map')
        .waitForNotification('suggest-draw-point')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)
        .then(() => role? this
                .debugLog('Changing master category to ' + role)
                .pointerClick(pageObject.geoObjMasterEditorViewSelect())
                .waitForVisible(pageObject.nkPopup.menuFocused())
                .selectElementByTextInMenu(i18n('roles', role))
                .waitForInvisible(pageObject.nkPopup.menuFocused()) :
            true
        )
        .then(() => linkName? this
                .debugLog('Linking to ' + linkName)
                .pointerClick(pageObject.geoObjEditorView.addLinkInput())
                .waitForVisible(pageObject.suggestPopup())
                .pointerClick(pageObject.suggestItem() + '=' + linkName)
                .waitForInvisible(pageObject.suggestPopup()) :
            true
        )
        .debugLog('Setting address name')
        .pointerClick(pageObject.group1() + ' ' + pageObject.nkTextInputControl())
        .setValue(pageObject.group1() + ' ' + pageObject.nkTextInputControl(), addressName)
        .pointerClick(pageObject.moreIcon())
        .waitForVisible(pageObject.addrNameLang())
        .selectNameAttrs(lang)
        .waitForInvisible(pageObject.submitDisabled());
}

function createVegetation() {
    return this
        .createGeoObject('vegetation')
        .debugLog('Drawing vegetation geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Setting vegetation name as ' + vegetationName)
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), vegetationName)
        .waitForInvisible(pageObject.submitDisabled())

        .saveGeoObject();
}

function createStreet() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 125)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 125)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Creating a street')
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), streetName)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())

        .saveGeoObject();
}

function createBuilding() {
    return this
        .createGeoObject('bld')
        .debugLog('Drawing building geometry')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .saveGeoObject();
}

function editAddress(newLinkName) {
    return this
        .debugLog('Editing address')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing assignment to ' + newLinkName)
        .pointerClick(pageObject.geoObjEditorView.addLinkInput.clear())
        .waitForInvisible(pageObject.geoObjEditorView.addLinkInput.clear())
        .pointerClick(pageObject.geoObjMasterEditorViewSelect())
        .waitForVisible(pageObject.nkPopup.menuFocused())
        .selectElementByTextInMenu(i18n('roles', 'master-addr_associated_with'))
        .waitForInvisible(pageObject.nkPopup.menuFocused())
        .pointerClick(pageObject.geoObjEditorView.addLinkInput())
        .waitForVisible(pageObject.suggestPopup())
        .pointerClick(pageObject.suggestItem() + '=' + newLinkName)
        .waitForInvisible(pageObject.suggestPopup())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'address-assigned-to-street-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'address-assigned-to-street-viewer')
        .verifyMapScreenshot(100, 135, 200, 165, 'address-assigned-on-map-selected')

        .checkFirstCommit('address-assigned')
        .closeSidebar();
}
