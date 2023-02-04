const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { UPDATE_TIMEOUT } = require('../../tools/constants'),
    station1 = {
        name: 'станция_метро_автотест_1',
        coords: [300, 310],
        nameLang: 'ru'
    },
    station2 = {
        name: 'станция_метро_автотест_2',
        coords: [330, 310],
        nameLang: 'ru'
    },
    station3 = {
        name: 'станция_метро_автотест_3',
        coords: [360, 310],
        nameLang: 'ru'
    },
    transferTime = 3000;

require('../common.js')(beforeEach, afterEach);

describe('transport transition', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('is drawn and changed', function() {
        return drawMetroStation.call(this.browser, station1)
            .then(() => drawMetroStation.call(this.browser, station2))
            .then(() => drawMetroStation.call(this.browser, station3))
            .moveMouseAside()
            .closeSidebar()
            .pointerClick(station1.coords[0], station1.coords[1])
            .waitForVisible(pageObject.geoObjViewerView())
            .pointerClick(pageObject.geoObjRelsView.link())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'transition-editor')
            .verifyMapScreenshot(260, 260, 140, 100, 'transition-1-on-map-selected')

            .debugLog('Select second station')
            .waitForNotification('suggest-select-slave-station_b')
            .pointerClick(station2.coords[0], station2.coords[1])
            .pause(UPDATE_TIMEOUT)
            .moveMouseAside()
            .verifyMapScreenshot(260, 260, 140, 100, 'transition-2-on-map-selected-1')

            .saveGeoObject()
            .waitForVisible(pageObject.geoObjViewerView())
            .closeSidebar()
            .verifyMapScreenshot(260, 260, 140, 100, 'transition-on-map-deselected-1')

            .debugLog('Changing transition...')
            .pointerClick(station2.coords[0], station2.coords[1])
            .waitForVisible(pageObject.geoObjViewerView())
            .pointerClick(pageObject.geoObjRelsViewItem.link())
            .waitForVisible(pageObject.sidebarHeaderTitle() + '=' + i18n('categories', 'transport_transition'))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'transition-viewer')
            .verifyMapScreenshot(260, 260, 140, 100, 'transition-2-on-map-selected-2')

            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Delete first station')
            .pointerClick(pageObject.transportTransitionEditorItem1.delete())
            .waitForNotification('suggest-select-slave-station_a')
            .debugLog('Select third station')
            .pointerClick(station3.coords[0], station3.coords[1])
            .waitForInvisible(pageObject.notification())

            .debugLog('Delete second station')
            .pointerClick(pageObject.transportTransitionEditorItem2.delete())
            .waitForNotification('suggest-select-slave-station_b')
            .debugLog('Select first station')
            .pointerClick(station1.coords[0], station1.coords[1])
            .waitForInvisible(pageObject.notification())

            .debugLog('Setting transition attrs...')
            .pointerClick([pageObject.group1(), pageObject.nkCheckboxControl1(), pageObject.nkCheckboxBox()].join(' ')) // checkbox transport_transition-oneway
            .pointerClick([pageObject.group2(), pageObject.nkCheckboxControl1(), pageObject.nkCheckboxBox()].join(' ')) // checkbox transport-is_virtual
            .pointerClick([pageObject.group2(), pageObject.nkCheckboxControl2(), pageObject.nkCheckboxBox()].join(' ')) // checkbox sys-not_operating
            .pointerClick([pageObject.group2(), pageObject.nkCheckboxControl3(), pageObject.nkCheckboxBox()].join(' ')) // checkbox sys-blocked
            .setValue(pageObject.textInputControl(), transferTime)
            .blurInput(pageObject.textInputControl())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'transition-changed-editor')
            .verifyMapScreenshot(260, 260, 140, 100, 'transition-changed-on-map-selected')

            .saveGeoObject()
            .waitForVisible(pageObject.geoObjViewerView())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'transition-changed-viewer')
            .closeSidebar()
            .verifyMapScreenshot(260, 260, 140, 100, 'transition-on-map-deselected-2');
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
        .setValue(pageObject.listCtrl.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang)

        .saveGeoObject();
}
