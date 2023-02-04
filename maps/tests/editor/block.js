const pageObject = require('../../page-object.js'),
    { COMMENTS_TIMEOUT, DRAW_TIMEOUT } = require('../../tools/constants'),
    bldAttrs = {
        ftType: 'bld-ft_type_id__173' // commerce
    };

require('../common.js')(beforeEach, afterEach);

describe('blocked object', function() {
    beforeEach(function() {
        return this.browser.initNmaps('yandex');
    });

    it('can be edited by yandex moderator', function() {
        return createBuilding.call(this.browser)
            .then(() => editBuilding.call(this.browser, bldAttrs))
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'object-on-map-deselected');
    });

    it('can\'t be edited by common user', function() {
        return createBuilding.call(this.browser)
            .ensureLogoutFast()
            .debugLog('Loggin in as a common user')
            .ensureLoggedInFast('common')
            .prepareNmaps()

            .pointerClick(160, 170)
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .pause(COMMENTS_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'block-common-viewer');
    });
});

function createBuilding() {
    return this
        .createGeoObject('bld')
        .debugLog('Drawing building geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Blocking building')
        .pointerClick(pageObject.nkCheckboxBox())

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'block-yandex-viewer');
}

function editBuilding(attrs) {
    return this
        .debugLog('Editing building')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.confirmationView())
        .pointerClick(pageObject.confirmationView.submit())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing building attributes...')
        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.nkCheckboxBox())
        .debugLog('Attributes changed')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'unblock-yandex-viewer');
}
