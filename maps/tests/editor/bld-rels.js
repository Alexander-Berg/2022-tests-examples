const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT, WAIT_FOR_CLASS_TIMEOUT } = require('../../tools/constants'),
    streetName = 'улица_автотест',
    lang = 'ru',
    entrances = [
        { name: '12', coords: [200, 200] },
        { name: '3А', coords: [200, 150] }],
    orgs = [
        { coords: [160, 175], category: ':nth-child(6)' },
        { coords: [175, 175], category: ':nth-child(7)' },
        { coords: [190, 175], category: ':nth-child(8)' },
        { coords: [205, 175], category: ':nth-child(9)' },
        { coords: [220, 175], category: ':nth-child(10)' },
        { coords: [235, 175], category: ':nth-child(11)' }];

require('../common.js')(beforeEach, afterEach);

describe('building', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('has address', function() {
        let objIndex = 0;

        return createStreet.call(this.browser, streetName)
            .then(() => createBuilding.call(this.browser))
            .then(() => addAddress.call(this.browser, ++objIndex))
            .then(() => addAddress.call(this.browser, ++objIndex))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'two-addresses-in-building-card')

            .then(() => revertAddress.call(this.browser))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'one-address-in-building-card')
            .closeSidebar();
    });

    it('has entrances', function() {
        let objIndex = 0;

        return createBuilding.call(this.browser)
            .then(() => addEntrance.call(this.browser, entrances[0], ++objIndex))
            .then(() => addEntrance.call(this.browser, entrances[1], ++objIndex))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'two-entrances-in-building-card')
            .verifyMapScreenshot(100, 100, 200, 200, 'two-entrances-on-map-deselected')

            .debugLog('Hovering entrance 1')
            .moveToObject(pageObject.geoObjRelsEntrance1())
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'entrance1-on-map-pin')

            .debugLog('Hovering entrance 2')
            .moveToObject(pageObject.geoObjRelsEntrance2())
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'entrance2-on-map-pin')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'two-entrances-and-building-on-map-deselected');
    });

    it('has main organization', function() {
        let objIndex = 0;

        return createBuilding.call(this.browser)
            .then(() => addOrganization.call(this.browser, orgs[0], ++objIndex))
            .then(() => addOrganization.call(this.browser, orgs[1], ++objIndex))
            .waitForVisible(pageObject.orgSummaryViewerViewItem.nkLink())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'two-organizations-in-building-card',
                { ignore: 'viewer-date' })
            .verifyMapScreenshot(100, 100, 200, 200, 'two-organizations-on-map-deselected')

            .then(() => setMainOrganization.call(this.browser))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'main-and-one-organizations-in-building-card',
                { ignore: 'viewer-date' })

            .debugLog('Hovering organization 1')
            .moveToObject(pageObject.orgSummaryViewerViewOrg1())
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'organization1-on-map-pin')

            .debugLog('Hovering organization 2')
            .moveToObject(pageObject.orgSummaryViewerViewOrg2())
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'organization2-on-map-pin')
            .checkHistoryCommits('building');
    });

    it('has over 5 organizations', function() {
        let browser = createBuilding.call(this.browser);
        for(let orgIndex = 0; orgIndex < 6; orgIndex++) {
            browser = addOrganization.call(browser, orgs[orgIndex], orgIndex + 1);
        }

        return browser
            .waitForVisible(pageObject.orgSummaryViewerViewItem.nkLink())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'six-organizations-in-building-card',
                { ignore: 'viewer-date' })
            .verifyMapScreenshot(100, 100, 200, 200, 'six-organizations-on-map-deselected')

            .then(() => setMainOrganization.call(browser))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'main-and-five-organizations-in-building-card',
                { ignore: 'viewer-date' })

            .debugLog('Checking possibility to add more organizations...')
            .pointerClick(pageObject.orgSummaryViewerViewOrg2())
            .waitForVisible(pageObject.geoObjOrgViewButton())
            .verifyScreenshot(pageObject.sidebarViewIsland(), 'full-organizations-list')
            .pointerClick(pageObject.geoObjOrgViewButton())
            .waitForVisible(pageObject.listItemView())
            .pointerClick(pageObject.listItemView() + ':nth-child(1)')
            .waitForVisible(pageObject.geoObjEditorView())
            .pointerClick(pageObject.geoObjEditorView.close())
            .waitForVisible(pageObject.sidebarHeader())
            .pointerClick(pageObject.sidebarHeader.close())
            .debugLog('Possibility checked');
    });
});

function createStreet(name) {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 125)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 125)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Creating street')
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .waitForVisible(pageObject.textInputFocused())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), name)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())

        .saveGeoObject();
}

function createBuilding() {
    return this
        .createGeoObject('bld')
        .debugLog('Drawing building geometry...')
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

function addAddress(objIndex) {
    return this
        .debugLog('Adding ' + objIndex + ' address...')
        .waitForVisible(pageObject.geoObjRelsAddAddress())
        .pointerClick(pageObject.geoObjRelsAddAddress())
        .waitForVisible(pageObject.notificationSuggest())

        .debugLog('Putting address on map')
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 135, 200, 130, 'address' + objIndex + '-drawn')

        .debugLog('Setting address name')
        .pointerClick(pageObject.geoObjEditorView.addLinkInput())
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .pointerClick(pageObject.group1() + ' ' + pageObject.nkTextInputControl())
        .setValue(pageObject.group1() + ' ' + pageObject.nkTextInputControl(), objIndex)
        .pointerClick(pageObject.moreIcon())
        .waitForVisible(pageObject.addrNameLang())
        .selectNameAttrs(lang)
        .waitForInvisible(pageObject.submitDisabled())

        .saveGeoObject()
        .waitForVisible(pageObject.geoObjRelsAddAddress())
        .verifyMapScreenshot(100, 135, 200, 130, 'address' + objIndex + '-on-map-deselected');
}

function revertAddress() {
    return this
        .debugLog('Reverting address...')
        .pointerClick(pageObject.geoObjRelsAddress1())
        .waitForVisible(pageObject.moreIcon())
        .pointerClick(pageObject.moreIcon())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('editor', 'revert-commits'))
        .waitForVisible(pageObject.commitView())
        .pointerClick(pageObject.commitView.button())
        .waitForVisible(pageObject.geoObjViewerView.deleted())
        .waitForVisible(pageObject.notificationSuccess())
        .debugLog('Address reverted')

        .verifyScreenshot(pageObject.geoObjViewerView(), 'address-deleted-viewer')
        .waitForExist(pageObject.geoObjViewerView.close(), WAIT_FOR_CLASS_TIMEOUT)
        .pointerClick(pageObject.geoObjViewerView.close())
        .pause(UPDATE_TIMEOUT);
}

function addEntrance(obj, objIndex) {
    return this
        .debugLog('Adding ' + objIndex + ' entrance')
        .waitForVisible(pageObject.geoObjRelsAddEntrance())
        .pointerClick(pageObject.geoObjRelsAddEntrance())
        .waitForVisible(pageObject.notificationSuggest())

        .debugLog('Putting entrance on map')
        .waitForNotification('suggest-drawing-poi-entrance-geometry')
        .pointerClick(obj.coords[0], obj.coords[1])
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'entrance' + objIndex + '-drawn')

        .debugLog('Setting entrance name')
        .pointerClick(pageObject.group2() + ' ' + pageObject.nkLink())
        .waitForVisible(pageObject.group2() + ' ' + pageObject.nkTextInputControl())
        .pointerClick(pageObject.group2() + ' ' + pageObject.nkTextInputControl())
        .setValue(pageObject.group2() + ' ' + pageObject.nkTextInputControl(), obj.name)
        .waitForInvisible(pageObject.submitDisabled())

        .saveGeoObject()
        .waitForVisible(pageObject.geoObjRelsAddEntrance())
        .verifyMapScreenshot(100, 100, 200, 200, 'entrance' + objIndex + '-on-map-deselected');
}

function addOrganization(obj, objIndex) {
    return this
        .debugLog('Adding ' + objIndex + ' organization')
        .waitForVisible(pageObject.orgSummaryViewerViewItem.addLink())
        .pointerClick(pageObject.orgSummaryViewerViewItem.addLink())
        .waitForVisible(pageObject.listItemView())

        .debugLog('Choosing organization and rubric')
        .pointerClick(pageObject.listItemView() + obj.category)
        .waitForVisible(pageObject.businessMainRubricEditorViewSuggest.input())
        .pointerClick(pageObject.businessMainRubricEditorViewSuggest.input())
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())

        .debugLog('Putting poi on map')
        .waitForNotification('suggest-drawing-poi-geometry')
        .pointerClick(obj.coords[0], obj.coords[1])
        .saveGeoObject();
}

function setMainOrganization() {
    return this
        .debugLog('Setting building main organization')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjMasterEditorViewSuggest.input())
        .pointerClick(pageObject.geoObjMasterEditorViewSuggest.input())
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .saveGeoObject();
}
