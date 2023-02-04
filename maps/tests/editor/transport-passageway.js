const pageObject = require('../../page-object.js'),
    { SUGGEST_TIMEOUT } = require('../../tools/constants'),
    i18n = require('../../tools/i18n.js'),
    station1 = {
        name: 'станция_метро_автотест_1',
        coords: [300, 310],
        nameLang: 'ru'
    },
    station2 = {
        name: 'станция_метро_автотест_2',
        coords: [360, 310],
        nameLang: 'ru'
    },
    exit1 = {
        name: '1',
        coords: [300, 360]
    },
    exit2 = {
        name: '2',
        coords: [360, 360]
    },
    transferTime = 3000;

require('../common.js')(beforeEach, afterEach);

describe('transport passageway', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('is drawn and changed', function() {
        return drawMetroStation.call(this.browser, station1)
            .then(() => drawMetroStation.call(this.browser, station2))
            .then(() => drawMetroExit.call(this.browser, exit1))
            .then(() => drawMetroExit.call(this.browser, exit2, station1.name))
            .verifyMapScreenshot(260, 260, 140, 105, 'metro-stations-exits-drawn')
            .closeSidebar()

            .debugLog('Add transport passageway to ' + station2.name)
            .pointerClick(station2.coords[0], station2.coords[1])
            .waitForVisible(pageObject.geoObjViewerView())
            .pointerClick(pageObject.geoObjRelsView3() + ' ' + pageObject.nkLinkViewAction())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'passageway-editor')
            .debugLog('Add ' + exit1.name)
            .pointerClick(exit1.coords[0], exit1.coords[1])
            .waitForInvisible(pageObject.notification())
            .moveMouseAside()
            .verifyMapScreenshot(260, 260, 140, 105, 'passageway-on-map-selected-1')
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'metro-station-viewer')
            .closeSidebar()
            .verifyMapScreenshot(260, 260, 140, 105, 'passageway-on-map-deselected-1')

            .debugLog('Changing passageway...')
            .pointerClick(station2.coords[0], station2.coords[1])
            .waitForVisible(pageObject.geoObjViewerView())
            .pointerClick(pageObject.geoObjRelsView3() + ' ' + pageObject.nkLink())
            .waitForVisible(pageObject.sidebarHeaderTitle() + '=' + i18n('categories', 'transport_passageway'))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'passageway-viewer')
            .verifyMapScreenshot(260, 260, 140, 105, 'passageway-on-map-selected-2')

            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Delete second station')
            .pointerClick(pageObject.transportTransitionEditorItem1.delete())
            .waitForNotification('suggest-select-slave-station')
            .debugLog('Select first station')
            .pointerClick(station1.coords[0], station1.coords[1])
            .waitForInvisible(pageObject.notification())

            .debugLog('Delete first exit')
            .pointerClick(pageObject.transportTransitionEditorItem2.delete())
            .waitForNotification('suggest-select-slave-exit')
            .debugLog('Select second exit')
            .pointerClick(exit2.coords[0], exit2.coords[1])
            .waitForInvisible(pageObject.notification())

            .debugLog('Setting passageway attrs...')
            .pointerClick(pageObject.radioButton3()) // direction – exit
            .pointerClick(pageObject.textInputControl())
            .setValue(pageObject.textInputControl(), transferTime)
            .pointerClick([pageObject.nkCheckboxControl1(), pageObject.nkCheckboxBox()].join(' ')) // checkbox transport-is_virtual
            .pointerClick([pageObject.nkCheckboxControl2(), pageObject.nkCheckboxBox()].join(' ')) // checkbox sys-not_operating
            .pointerClick([pageObject.nkCheckboxControl3(), pageObject.nkCheckboxBox()].join(' ')) // checkbox sys-blocked
            .verifyScreenshot(pageObject.geoObjEditorView(), 'passageway-changed-editor')
            .verifyMapScreenshot(260, 260, 140, 105, 'passageway-changed-on-map-selected')

            .saveGeoObject()
            .waitForVisible(pageObject.geoObjViewerView())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'passageway-changed-viewer')
            .closeSidebar();
    });
});

function drawMetroStation(attrs) {
    return this.createGeoObject('transport_metro_station')
        .debugLog('Starting to draw a station ' + attrs.name + '...')
        .waitForNotification('suggest-draw-point')
        .pointerClick(attrs.coords[0], attrs.coords[1])

        .debugLog('Add station name')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.listCtrl.textInputControl())
        .pointerClick(pageObject.listCtrl.textInputControl())
        .setValue(pageObject.listCtrl.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang)

        .saveGeoObject();
}

function drawMetroExit(attrs, linkName) {
    return this.createGeoObject('transport_metro_exit')
        .debugLog('Starting to draw a station...')
        .waitForNotification('suggest-draw-point')
        .pointerClick(attrs.coords[0], attrs.coords[1])

        .debugLog('Add exit name')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.listCtrl.textInputControl())
        .pointerClick(pageObject.listCtrl.textInputControl())
        .setValue(pageObject.listCtrl.textInputControl(), attrs.name)

        .then(() => linkName? linkMetroExit.call(this, linkName) : true)

        .saveGeoObject();
}

function linkMetroExit(name) {
    return this
        .debugLog(`Link exit to ${name} station`)
        .pointerClick(pageObject.geoObjMasterEditorViewSuggest.input())
        .setValue(pageObject.geoObjMasterEditorViewSuggest.input(), name)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .debugLog(`Metro line ${name} selected from suggest`);
}
