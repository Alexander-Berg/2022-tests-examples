const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    bldAttrs = {
        ftType: 'urban_areal-ft_type_id__114', // religion
        condition: 'bld-cond__2', // unowned
        height: '12',
        linkTo: 'улица_модератор_автотест',
        addressName: '7',
        lang: 'ru'
    };

require('../common.js')(beforeEach, afterEach);

describe('copy of object', function() {
    beforeEach(function() {
        return this.browser.initNmaps('moderator');
    });

    it('is done and deleted', function() {
        return drawRoad.call(this.browser, bldAttrs.linkTo)
            .then(() => drawBldWithAttrs.call(this.browser, bldAttrs))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'building-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'building-on-map-selected')

            .debugLog('Сopying building')
            .pointerClick(pageObject.moreIcon())
            .waitForVisible(pageObject.nkPopup())
            .selectElementByTextInMenu(i18n('common', 'copy'))
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'building-copy-drawn')

            .debugLog('Moving copy')
            .mouseDrag([250, 250], [280, 280])
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 100, 250, 250, 'building-copy-changed-drawn')

            .verifyScreenshot(pageObject.geoObjEditorView(), 'building-copy-editor')
            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'building-copy-viewer')
            .verifyMapScreenshot(100, 100, 250, 250, 'building-copy-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 250, 250, 'building-copy-on-map-deselected')

            .debugLog('Deleting building copy')
            .pointerClick(280, 280)
            .waitForVisible(pageObject.geoObjViewerView())
            .pointerClick(pageObject.moreIcon())
            .waitForVisible(pageObject.nkPopup())
            .selectElementByTextInMenu(i18n('common', 'delete'))
            .waitForVisible(pageObject.confirmationView.submit())
            .pointerClick(pageObject.confirmationView.submit())
            .waitForVisible(pageObject.notificationSuccess())
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'building-no-copy-on-map-deselected');
    });
});

function drawRoad(streetName) {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 125)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 125)
        .pause(DRAW_TIMEOUT)

        .debugLog('Creating a street')
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), streetName)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .pointerClick(pageObject.suggestPopup.desc())
        .moveMouseAside()
        .waitForInvisible(pageObject.suggestPopup())

        .saveGeoObject();
}

function drawBldWithAttrs(attrs) {
    return this
        .createGeoObject('bld')

        .debugLog('Starting to draw a building...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)

        .debugLog('Adding internal contour...')
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem2())
        .pause(HINT_TIMEOUT)
        .pointerClick(pageObject.ymapsItem2())
        .pause(DRAW_TIMEOUT)
        .pointerClick(175, 175)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 175)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 225)
        .pause(DRAW_TIMEOUT)
        .pointerClick(175, 225)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'building-drawn')

        .debugLog('Setting attributes...')
        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.geoObjEditorView.condition())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values',attrs.condition))
        .pointerClick(pageObject.inputControl.input())
        .setValue(pageObject.inputControl.input(), attrs.height)

        .saveGeoObject()

        .debugLog('Linking to address...')
        .pointerClick(pageObject.geoObjRelsAddAddress())
        .waitForVisible(pageObject.geoObjEditorView())
        .pointerClick(pageObject.geoObjEditorView.addLinkInput())
        .waitForVisible(pageObject.suggestPopup())
        .pointerClick(pageObject.suggestItem() + '=' + attrs.linkTo)
        .waitForInvisible(pageObject.suggestPopup())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.addressName)

        .pointerClick(pageObject.moreIcon())
        .waitForVisible(pageObject.addrNameLang())
        .selectNameAttrs(attrs.lang)
        .waitForInvisible(pageObject.submitDisabled())

        .saveGeoObject();
}
