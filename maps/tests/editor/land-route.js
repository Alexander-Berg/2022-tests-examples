const pageObject = require('../../page-object.js');
const i18n = require('../../tools/i18n.js');
const { ANIMATION_TIMEOUT, DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants');
const MAX_ZOOM = 22;
const TRANSPORT_NAME = 'транспорт_автотест';
const stop1 = {
    name: 'остановка1_автотест',
    nameLang: 'en',
    coords: [200, 200]
};
const stop2 = {
    name: 'остановка2_автотест',
    nameLang: 'ru',
    coords: [250, 200]
};
const stop3 = {
    name: 'остановка3_автотест',
    coords: [300, 200]
};

require('../common.js')(beforeEach, afterEach);

describe('land transport route', function() {
    beforeEach(function() {
        return this.browser
            .initNmaps('common')
            .setMapCenterByTestNumber(MAX_ZOOM);
    });

    it('is drawn', function() {
        const browser = this.browser;
        return drawLandTransportStop.call(browser, stop1)
            .then(() => linkTransportToStop.call(browser, pageObject.landStopSbView.bus.input(), TRANSPORT_NAME))
            .saveGeoObject()
            .then(() => drawLandTransportStop.call(browser, stop2))
            .saveGeoObject()
            .then(() => drawLandTransportStop.call(browser, stop3))
            .debugLog('Link existing bus')
            .pointerClick(pageObject.landStopSbView.bus.input())
            .waitForVisible(pageObject.suggestPopup.items())
            .pointerClick(pageObject.suggestPopup.item())
            .saveGeoObject()
            .verifyMapScreenshot(100, 150, 300, 100, 'land-route-stop3-on-map-selected')

            .debugLog('Editing land transport route...')
            .pointerClick(pageObject.transportRoutesViewerItem())
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'land-route-viewer')
            .pointerClick(pageObject.editIcon())
            .setMapCenterByTestNumber(MAX_ZOOM)
            .waitForVisible(pageObject.geoObjEditorView())
            .waitForNotification('suggest-select-slaves-transport_stop')
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 150, 300, 100, 'land-route-before-changed-drawn')

            .debugLog('Changing type')
            .moveMouseAside()
            .pointerClick(pageObject.geoObjEditorView.ftType())
            .waitForVisible(pageObject.nkPopup.menuFocused())
            .pause(ANIMATION_TIMEOUT)
            .verifyScreenshot(pageObject.nkPopup.menuFocused(), 'land-route-type-popup')
            .selectElementByTextInMenu(i18n('attr-values', 'transport_bus_route-ft_type_id__2202')) // trolley
            .waitForInvisible(pageObject.nkPopup.menuFocused())
            .pause(ANIMATION_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjEditorView(), 'land-route-changed-editor')

            .debugLog('Changing stops')
            .pointerClick(stop1.coords[0], stop1.coords[1])
            .pause(DRAW_TIMEOUT)
            .pointerClick(stop2.coords[0], stop2.coords[1])
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', stop2.coords[0], stop2.coords[1])
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 150, 450, 100, 'land-route-after-changed-drawn', { tolerance: 75 })

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'land-route-changed-viewer')
            .verifyMapScreenshot(100, 150, 300, 100, 'land-route-changed-on-map-selected')
            .pointerClick(pageObject.geoObjViewerView.close())
            .waitForVisible(pageObject.geoObjViewerView.close())
            .closeSidebar()
            .verifyMapScreenshot(100, 150, 300, 100, 'land-route-changed-on-map-deselected')

            .debugLog('Checking hints')
            .moveToObject('body', stop1.coords[0], stop1.coords[1])
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 210, 300, 70, 'land-route-stop-not-linked-hint')
            .moveToObject('body', stop2.coords[0], stop2.coords[1])
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 210, 450, 70, 'land-route-stop-linked-hint')
            .debugLog('Checking viewers')
            .pointerClick(stop1.coords[0], stop1.coords[1])
            .waitForVisible(pageObject.geoObjViewerView())
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'land-route-stop-not-linked-viewer')
            .closeSidebar()
            .pointerClick(stop2.coords[0], stop2.coords[1])
            .waitForVisible(pageObject.geoObjViewerView())
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'land-route-stop-linked-viewer');
    });
});

function drawLandTransportStop(attrs) {
    return this.createGeoObject('transport_stop')
        .debugLog('Drawing transport stop geometry...')
        .waitForNotification('suggest-draw-point')
        .pointerClick(attrs.coords[0], attrs.coords[1])
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Setting transport stop attributes...')
        .debugLog('Setting name ' + attrs.name)
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .then(() => attrs.nameLang? this.selectNameAttrs(attrs.nameLang) : true)
        .debugLog('Attributes set');
}

function linkTransportToStop(inputSelector, name) {
    return this
        .debugLog('Link transport "' + name + '" to stop')
        .pointerClick(inputSelector)
        .setValue(inputSelector, name)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .pause(UPDATE_TIMEOUT);
}
