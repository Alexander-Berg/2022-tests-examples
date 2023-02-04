const pageObject = require('../../page-object.js');
const i18n = require('../../tools/i18n.js');
const { DRAW_TIMEOUT } = require('../../tools/constants');
const MAX_ZOOM = 22;
const outsourceAttrs = {
    tracker: 'OUTKART-1'
};
const outsourceAttrsChanged = {
    name: 'за_автотест',
    type: 'outsource_region-task_type__traffic_light_from_pano',
    ratio: '2',
    company: 'mercator',
    login: 'confeta.test',
    quality: 'outsource_region-quality__bad',
    tracker: 'OUTKART-2',
    status: 'outsource_region-status__can_start'
};

require('../common.js')(beforeEach, afterEach);

describe('outsource region', function() {
    beforeEach(function() {
        return this.browser
            .initNmaps('cartographer')
            .setMapCenterByTestNumber(MAX_ZOOM);
    });

    it('is drawn and changed', function() {
        return createOutsourceRegion.call(this.browser, outsourceAttrs)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'outsource-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'outsource-on-map-selected', { tolerance: 75 })
            .then(() => editOutsourceRegion.call(this.browser, outsourceAttrsChanged));
    });
});

function createOutsourceRegion(attrs) {
    return this
        .createGeoObjectBySuggest('outsource_region')
        .debugLog('Drawing outsource region geometry...')
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

        .debugLog('Setting outsource region attributes...')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'outsource-editor')
        .pointerClick(pageObject.group3() + ' ' + pageObject.control5() + ' ' + pageObject.input())
        .setValue(pageObject.group3() + ' ' + pageObject.control5() + ' ' + pageObject.input(), attrs.tracker)
        .debugLog('Attributes set')

        .saveGeoObject();
}

function editOutsourceRegion(attrs) {
    return this
        .debugLog('Editing outsource region')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing outsource region attributes...')
        .pointerClick(pageObject.group1() + ' ' + pageObject.control1() + ' ' + pageObject.input())
        .setValue(pageObject.group1() + ' ' + pageObject.control1() + ' ' + pageObject.input(), attrs.name)
        .pointerClick(pageObject.group1() + ' ' + pageObject.select.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', attrs.type))

        .pointerClick(pageObject.group2() + ' ' + pageObject.checkboxGroupItem2())
        .pointerClick(pageObject.group2() + ' ' + pageObject.checkboxGroupItem3())

        .pointerClick(pageObject.group3() + ' ' + pageObject.control1() + ' ' + pageObject.input())
        .setValue(pageObject.group3() + ' ' + pageObject.control1() + ' ' + pageObject.input(), attrs.ratio)
        .pointerClick(pageObject.group3() + ' ' + pageObject.control2() + ' ' + pageObject.input())
        .setValue(pageObject.group3() + ' ' + pageObject.control2() + ' ' + pageObject.input(), attrs.company)
        .pointerClick(pageObject.group3() + ' ' + pageObject.control3() + ' ' + pageObject.input())
        .setValue(pageObject.group3() + ' ' + pageObject.control3() + ' ' + pageObject.input(), attrs.login)
        .pointerClick(pageObject.group3() + ' ' + pageObject.control4() + ' ' + pageObject.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', attrs.quality))
        .pointerClick(pageObject.group3() + ' ' + pageObject.control5() + ' ' + pageObject.input())
        .setValue(pageObject.group3() + ' ' + pageObject.control5() + ' ' + pageObject.input(), attrs.tracker)
        .pointerClick(pageObject.group3() + ' ' + pageObject.control6() + ' ' + pageObject.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', attrs.status))
        .debugLog('Attributes changed')

        .debugLog('Changing outsource region geometry...')
        .leftClick('body', 150, 150)
        .leftClick('body', 150, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry changed')
        .verifyMapScreenshot(100, 100, 200, 200, 'outsource-changed-drawn', { tolerance: 75 })

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'outsource-changed-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'outsource-changed-on-map-selected', { tolerance: 75 })

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'outsource-changed-on-map-deselected');
}
