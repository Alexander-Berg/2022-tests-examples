const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants'),
    reliefAttrs = {
        name: 'гора_автотест',
        nameLang: 'ru'
    },
    reliefAttrsChanged = {
        name: 'вулкан_автотест',
        nameType: 'address_label',
        nameLang: 'en',
        ftType: 'relief_point-ft_type_id__308' // volcano
    };

require('../common.js')(beforeEach, afterEach);

describe('relief-point', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return drawReliefPoint.call(this.browser, reliefAttrs)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'relief-point-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'relief-point-on-map-selected')
            .then(() => editReliefPoint.call(this.browser, reliefAttrsChanged));
    });
});

function drawReliefPoint(attrs) {
    return this
        .createGeoObject('relief_point/ft_type_id__306') // peak
        .waitForNotification('suggest-draw-point')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)

        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang)
        .verifyMapScreenshot(100, 100, 200, 200, 'relief-point-drawn')

        .saveGeoObject();
}

function editReliefPoint(attrs) {
    return this
        .debugLog('Editing relief')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing relief geometry')
        .waitForNotification('suggest-edit-point')
        .pointerClick(250, 200)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'relief-point-changed-drawn')

        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang, attrs.nameType)

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'relief-point-changed-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'relief-point-changed-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'relief-point-changed-on-map-deselected');
}
