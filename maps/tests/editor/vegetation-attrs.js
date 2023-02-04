const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT, WAIT_FOR_CLASS_TIMEOUT } = require('../../tools/constants'),
    vegetationAttrs = {
        ftType: 'vegetation-ft_type_id__402', // park
        name: 'парк_автотест',
        nameLang: 'ru'
    },
    vegetationAttrsChanged = {
        ftType: 'vegetation-ft_type_id__406', // urban vegetation
        name: 'внутриквартальная_автотест',
        nameType: 'synonym',
        nameLang: 'en'
    };

require('../common.js')(beforeEach, afterEach);

describe('vegetation', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn by default', function() {
        return createVegetation.call(this.browser)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'vegetation-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-on-map-deselected');
    });

    it('is drawn with attributes and changed', function() {
        return createVegetation.call(this.browser, vegetationAttrs)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'vegetation-with-attrs-viewer')
            .then(() => changeVegetationAttrs.call(this.browser, vegetationAttrsChanged));
    });

    it('is saved with hotkey ctrl+shift+a', function() {
        return createVegetation.call(this.browser, vegetationAttrs, true);
    });
});

function createVegetation(attrs, isHotkey) {
    return this
        .debugLog('Creating a vegetation')
        .createGeoObject('vegetation')

        .debugLog('Drawing vegetation geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .then(() => attrs? this
            .debugLog('Setting attributes...')
                .selectFtType(attrs.ftType)
                .pointerClick(pageObject.nkLinkViewAction())
                .waitForVisible(pageObject.textInputControl())
                .pointerClick(pageObject.textInputControl())
                .setValue(pageObject.textInputControl(), attrs.name)
                .selectNameAttrs(attrs.nameLang)
                .debugLog('Attributes set') :
            true
        )

        .then(() => isHotkey?
            this.keys(['Control', 'Shift', 'a'])
                .debugLog('Vegetation is saved by hotkey. New object of the same type is ready for drawing.')
                .waitForExist(pageObject.geoObjEditorView.submitDisabled(),
                    WAIT_FOR_CLASS_TIMEOUT)
                .waitForVisible(pageObject.geoObjEditorView())
                .verifyScreenshot(pageObject.geoObjEditorView(), 'vegetation-hotkey-editor') :
            this.moveMouseAside()
                .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-drawn')
                .saveGeoObject()
        );
}

function changeVegetationAttrs(attrs) {
    return this
        .debugLog('Editing vegetation')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing attributes...')
        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.listCtrlItem() + ':nth-child(3) ' + pageObject.nkTextInputControl())
        .setValue(pageObject.listCtrlItem() + ':nth-child(3) ' + pageObject.nkTextInputControl(), attrs.name)
        .debugLog('Attributes changed')

        .saveGeoObject()
        .pointerClick(pageObject.actionExpand())
        .pause(UPDATE_TIMEOUT)
        .verifyScreenshot(pageObject.geoObjViewerView(), 'vegetation-changed-viewer');
}
