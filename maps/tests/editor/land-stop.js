const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    STOP_NAME = 'остановка_автотест',
    BUS1_NAME = 'автобус1_автотест',
    BUS2_NAME = 'автобус2_автотест',
    TRAM_NAME = 'трамвай_автотест',
    TROLLEY_NAME = 'троллейбус_автотест',
    MINIBUS1_NAME = 'маршрутка1_автотест',
    MINIBUS2_NAME = 'маршрутка2_автотест';

require('../common.js')(beforeEach, afterEach);

describe('land transport stop', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn', function() {
        return drawLandTransportStop.call(this.browser, STOP_NAME)
            .verifyMapScreenshot(100, 100, 200, 200, 'land-stop-drawn')
            .then(() => linkTransportToStop.call(this.browser, pageObject.landStopSbView.bus.input(), BUS1_NAME))
            .then(() => linkTransportToStop.call(this.browser, pageObject.landStopSbView.tram.input(), TRAM_NAME))
            .then(() => linkTransportToStop.call(this.browser, pageObject.landStopSbView.mini.input(), MINIBUS1_NAME))
            .saveGeoObject()
            .isVisible(pageObject.sidebarView.island2())
            .then(visible => visible || this.browser.reportError('Adding panel is not visible'))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'land-stop-viewer')
            .verifyScreenshot(pageObject.sidebarView.island2(), 'land-stop-add-section')

            .debugLog('Adding second stop')
            .pointerClick(pageObject.sidebarView.island2.listItemView())
            .waitForVisible(pageObject.geoObjEditorView())
            .waitForNotification('suggest-draw-point')
            .pointerClick(250, 200)
            .pause(DRAW_TIMEOUT)
            .pointerClick(pageObject.deleteIconSmall())
            .waitForInvisible(pageObject.deleteIconSmall())
            .pointerClick(pageObject.landStopSbView.mini() + ' ' + pageObject.closeIconSmall())
            .pause(UPDATE_TIMEOUT)
            .saveGeoObject()
            .isVisible(pageObject.sidebarView.island2())
            .then(visible => visible || this.browser.reportError('Adding panel is not visible'))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'land-stop-second-viewer')
            .verifyMapScreenshot(150, 100, 150, 150, 'land-stop-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(150, 100, 150, 150, 'land-stop-on-map-deselected');
    });

    it('is changed', function() {
        return drawLandTransportStop.call(this.browser, STOP_NAME)
            .then(() => linkTransportToStop.call(this.browser, pageObject.landStopSbView.bus.input(), BUS1_NAME))
            .then(() => linkTransportToStop.call(this.browser, pageObject.landStopSbView.tram.input(), TRAM_NAME))
            .then(() => linkTransportToStop.call(this.browser, pageObject.landStopSbView.troll.input(), TROLLEY_NAME))
            .then(() => linkTransportToStop.call(this.browser, pageObject.landStopSbView.mini.input(), MINIBUS1_NAME))
            .saveGeoObject()

            .debugLog('Editing transport stop')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing transport stop attributes...')
            .debugLog('Deleting existing transport')
            .pointerClick(pageObject.landStopSbView.tram.deleteIcon())
            .waitForInvisible(pageObject.landStopSbView.tram.deleteIcon())
            .pointerClick(pageObject.landStopSbView.troll.deleteIcon())
            .waitForInvisible(pageObject.landStopSbView.troll.deleteIcon())
            .pointerClick(pageObject.landStopSbView.mini.deleteIcon())
            .waitForInvisible(pageObject.landStopSbView.mini.deleteIcon())
            .debugLog('Adding transport')
            .then(() => linkTransportToStop.call(this.browser, pageObject.landStopSbView.bus.input(), BUS2_NAME))
            .then(() => linkTransportToStop.call(this.browser, pageObject.landStopSbView.mini.input(), MINIBUS2_NAME))
            .debugLog('Link existing tram')
            .pointerClick(pageObject.landStopSbView.tram.input())
            .waitForVisible(pageObject.suggestPopup.items())
            .pointerClick(pageObject.suggestPopup.item())
            .debugLog('Attributes changed')

            .debugLog('Changing transport stop geometry...')
            .pointerClick(250, 200)
            .pause(DRAW_TIMEOUT)
            .debugLog('Geometry changed')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'land-stop-changed-editor')
            .verifyMapScreenshot(100, 100, 200, 200, 'land-stop-changed-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'land-stop-changed-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'land-stop-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'land-stop-changed-on-map-deselected')

            .moveToObject('body', 250, 200)
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(200, 100, 450, 250, 'land-stop-changed-on-map-hint');
    });
});

function drawLandTransportStop(name) {
    return this.createGeoObject('transport_stop')
        .debugLog('Drawing transport stop geometry...')
        .waitForNotification('suggest-draw-point')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Setting transport stop attributes...')
        .debugLog('Setting name ' + name)
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), name)
        .selectNameAttrs('ru')
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
