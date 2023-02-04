const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants'),
    poiDefaultRubric = ':nth-child(3)',
    poiAttrs = {
        name: 'пои_автотест',
        rubric: poiDefaultRubric,
        nameLang: 'ru'
    },
    poiAttrsChanged = {
        category: 'poi_sport',
        ftType: 'poi_sport-ft_type_id__191', // sports facility
        rubric: ':nth-child(3)',
        input: '<script>alert(1)</script>',
        nameType: 'render_label',
        nameLang: 'en'
    };

require('../common.js')(beforeEach, afterEach);

describe('poi', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn by default', function() {
        return drawPoi.call(this.browser, 'poi_shopping')
            .verifyMapScreenshot(100, 100, 200, 200, 'poi-default-drawn')
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'poi-default-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'poi-default-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'poi-default-on-map-deselected');
    });

    it('is drawn with attributes and changed', function() {
        return drawPoi.call(this.browser, 'poi_government', poiAttrs)
            .verifyMapScreenshot(100, 100, 200, 200, 'poi-full-drawn')
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'poi-full-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'poi-full-on-map-selected')
            .then(() => editPoi.call(this.browser, poiAttrsChanged));
    });

    // TODO add fake Sprav organization to link to
});

function drawPoi(category, attrs) {
    return this
        .createGeoObject(category)
        .waitForNotification('suggest-drawing-poi-geometry')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)

        .pointerClick(pageObject.poiSbView.rubricInput())
        .waitForVisible(pageObject.suggestPopup())
        .pointerClick(pageObject.suggestPopup.item() + poiDefaultRubric)

        .then(() => attrs?
            this.debugLog('Testing that business suggest looks as expected')
                .pointerClick(pageObject.businessEditorViewSuggest.input())
                .waitForVisible(pageObject.suggestPopup.item())
                .verifyScreenshot(pageObject.geoObjEditorView(), 'poi-full-no-sprav-match')

                .debugLog('Setting poi name')
                .pointerClick(pageObject.nkLinkViewAction())
                .waitForVisible(pageObject.textInputControl())
                .setValue(pageObject.listCtrl() + ' ' + pageObject.nkTextInputControl(), attrs.name)
                .selectNameAttrs(attrs.nameLang) :
            true
        );
}

function editPoi(attrs) {
    return this
        .debugLog('Editing poi')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing poi geometry')
        .waitForNotification('suggest-editing-poi-geometry')
        .pointerClick(175, 225)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'poi-changed-drawn')

        .debugLog('Changing poi category')
        .pointerClick(pageObject.changeCategoryIcon())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('categories', attrs.category))
        .waitForNotification('suggest-editing-poi-geometry')

        .debugLog('Changing poi attributes...')
        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.poiSbView.rubricInput())
        .waitForVisible(pageObject.suggestPopup())
        .pointerClick(pageObject.suggestPopup.item() + attrs.rubric)

        .pointerClick(pageObject.listCtrlItem() + ' ' + pageObject.nkTextInputControl())
        .setValue(pageObject.listCtrlItem() + ' ' + pageObject.nkTextInputControl(), attrs.input)
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.listCtrlItem() + ':nth-child(3) ' + pageObject.nkTextInputControl())
        .setValue(pageObject.listCtrlItem() + ':nth-child(3) ' + pageObject.nkTextInputControl(), attrs.input)
        .selectNameAttrs(attrs.nameLang, attrs.nameType)
        .debugLog('Attributes changed')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'poi-changed-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'poi-changed-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'poi-changed-on-map-deselected');
}
