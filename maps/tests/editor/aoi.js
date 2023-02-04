const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants'),
    MAX_ZOOM = 22,
    aoiAttrs = {
        name: 'зк_автотест',
        desc: 'описание_автотест'
    },
    aoiAttrsChanged = {
        name: 'зк_автотест_новая',
        state: 'aoi-type__1',
        desc: 'описание_автотест_новая'
    };

require('../common.js')(beforeEach, afterEach);

describe('aoi', function() {
    beforeEach(function() {
        return this.browser
            .initNmaps('cartographer')
            .setMapCenterByTestNumber(MAX_ZOOM);
    });

    it('is drawn and changed', function() {
        return createAoi.call(this.browser, aoiAttrs)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'aoi-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'aoi-on-map-selected')
            .then(() => editAoi.call(this.browser, aoiAttrsChanged));
    });
});

function createAoi(attrs) {
    return this
        .createGeoObjectBySuggest('aoi')
        .debugLog('Drawing aoi geometry...')
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

        .debugLog('Setting aoi attributes...')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'aoi-editor')
        .pointerClick(pageObject.inputControl.input())
        .setValue(pageObject.inputControl.input(), attrs.name)
        .pointerClick(pageObject.textarea())
        .setValue(pageObject.textarea(), attrs.desc)
        .debugLog('Attributes set')

        .saveGeoObject();
}

function editAoi(attrs) {
    return this
        .debugLog('Editing aoi')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing aoi geometry...')
        .moveToObject('body', 200, 150)
        .pause(DRAW_TIMEOUT)
        .mouseDrag([200, 150], [200, 175])
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry changed')

        .debugLog('Changing aoi attributes...')
        .pointerClick(pageObject.inputControl.input())
        .setValue(pageObject.inputControl.input(), attrs.name)
        .pointerClick(pageObject.select.button())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', attrs.state))

        .pointerClick(pageObject.textarea())
        .setValue(pageObject.textarea(), attrs.desc)
        .debugLog('Attributes changed')
        .verifyMapScreenshot(100, 100, 200, 200, 'aoi-changed-drawn')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'aoi-changed-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'aoi-changed-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'aoi-changed-on-map-deselected');
}
