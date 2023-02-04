const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants'),
    hydroAttrs = {
        name: 'ключ_автотест',
        nameLang: 'ru'
    },
    hydroAttrsChanged = {
        ftType: 'hydro_point-ft_type_id__512', // fountain
        name: 'фонтан_автотест',
        nameLang: 'en',
        nameType: 'address_label'
    };

require('../common.js')(beforeEach, afterEach);

describe('hydro-point', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return createHydroPoint.call(this.browser, hydroAttrs)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'hydro-point-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'hydro-point-on-map-selected')
            .then(() => editHydroPoint.call(this.browser, hydroAttrsChanged));
    });
});

function createHydroPoint(attrs) {
    return this
        .createGeoObject('hydro_point')
        .debugLog('Drawing hydro-point geometry...')
        .pause(DRAW_TIMEOUT)
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-point-drawn')

        .debugLog('Setting hydro-point attributes...')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang)
        .debugLog('Attributes set')

        .saveGeoObject();
}

function editHydroPoint(attrs) {
    return this
        .debugLog('Editing hydro-point')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing hydro-point attributes...')
        .selectFtType(attrs.ftType)
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang, attrs.nameType)
        .debugLog('Attributes changed')

        .debugLog('Changing hydro-point geometry...')
        .pointerClick(200, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry changed')
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-point-changed-drawn')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'hydro-point-changed-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-point-changed-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-point-changed-on-map-deselected');
}
