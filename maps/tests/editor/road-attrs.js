const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants'),
    roadAttrs = {
        speed: '60',
        ftType: 'rd_el-fc__4'
    };

require('../common.js')(beforeEach, afterEach);

describe('road', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn by default', function() {
        return createRoad.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'road-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'road-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'road-on-map-deselected');
    });

    it('is drawn with attributes and changed', function() {
        return createRoad.call(this.browser, roadAttrs)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'road-with-attrs-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'road-with-attrs-on-map-selected')

            .debugLog('Editing road')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .moveMouseAside()

            .debugLog('Changing road attributes...')
            .selectFtType(roadAttrs.ftType)
            .pointerClick(pageObject.control2() + ' ' + pageObject.radioButton2()) // A to B
            .pointerClick(pageObject.nkCheckboxControl1() + ' ' + pageObject.nkCheckboxBox()) // check paved
            .pointerClick(pageObject.nkCheckboxControl2() + ' ' + pageObject.nkCheckboxBox()) // check bad condition
            .pointerClick(pageObject.nkCheckboxControl3() + ' ' + pageObject.nkCheckboxBox()) // check back bus
            .pointerClick(pageObject.busIcon()) // disable bus
            .pointerClick(pageObject.bikeIcon()) // disable bike
            .pointerClick(pageObject.taxiIcon()) // disable taxi
            .pointerClick(pageObject.control5() + ' ' + pageObject.button())
            .waitForVisible(pageObject.nkPopup.menuFocused())
            .selectElementByTextInMenu(i18n('attr-values', 'rd_el-struct_type__2')) // tunnel

            .pointerClick(pageObject.control6() + ' ' + pageObject.button())
            .waitForVisible(pageObject.nkPopup.menuFocused())
            .selectElementByTextInMenu(i18n('attr-values', 'rd_el-fow__10')) // ramp
            .pointerClick(pageObject.link() + ':nth-child(4)') // 40 km/h
            .debugLog('Attributes changed')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'road-changed-editor')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'road-changed-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'road-changed-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'road-changed-on-map-deselected');
    });
});

function createRoad(attrs) {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 100, 200, 200, 'road-drawn')

        .then(() => attrs? this
            .debugLog('Setting attributes...')
                .pointerClick(pageObject.nkCheckboxControl1() + ' ' + pageObject.nkCheckboxBox()) // uncheck paved
                .pointerClick(pageObject.nkCheckboxControl3() + ' ' + pageObject.nkCheckboxBox()) // check residential
                .pointerClick(pageObject.group2() + ' ' + pageObject.nkCheckboxBox()) // check under construction
                .pointerClick(pageObject.pedestrianIcon()) // enable pedestrian
                .pointerClick(pageObject.bikeIcon()) // enable bike
                .pointerClick(pageObject.control7() + ' ' + pageObject.input())
                .setValue(pageObject.control7() + ' ' + pageObject.input(), attrs.speed)
                .blurInput(pageObject.control7() + ' ' + pageObject.input())
                .verifyScreenshot(pageObject.geoObjEditorView(), 'road-with-attrs-editor') :
            true
        )
        .saveGeoObject();
}
