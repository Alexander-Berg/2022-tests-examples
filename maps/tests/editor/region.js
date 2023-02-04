const pageObject = require('../../page-object.js'),
    { COMMENTS_TIMEOUT, DRAW_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    regionAttrs = {
        name: 'регион_автотест',
        nameLang: 'ru'
    },
    adAttrs = {
        name: 'атд_автотест',
        nameLang: 'ru',
        ftType: 'ad-level_kind__1'
    },
    neutralAttrs = {
        name: 'нейтральные_воды_автотест',
        nameLang: 'ru'
    };

require('../common.js')(beforeEach, afterEach);

describe('region', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('is drawn and linked to ad and ad_neutral', function() {
        return createRegion.call(this.browser, regionAttrs)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'region-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'region-on-map-selected')
            .then(() => createAd.call(this.browser, adAttrs))
            .then(() => createNeutral.call(this.browser, neutralAttrs))
            .pointerClick(200, 200)
            .waitForVisible(pageObject.geoObjViewerView.commentsLink())
            .pause(COMMENTS_TIMEOUT)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'region-linked-viewer');
    });
});

function createRegion(attrs) {
    return this
        .createGeoObjectBySuggest('region')
        .debugLog('Drawing region geometry...')
        .waitForNotification('suggest-draw-point')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Setting region attributes...')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'region-editor')
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang)
        .debugLog('Attributes set')

        .saveGeoObject();
}

function createAd(attrs) {
    return this
        .createGeoObject('ad')
        .debugLog('Drawing ad geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(150, 125)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 125)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 185)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 185)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 125)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Setting ad attributes...')
        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.fieldsetCtrl1.input())
        .setValue(pageObject.fieldsetCtrl1.input(), attrs.name)
        .selectNameAttrs(attrs.nameLang)
        .debugLog('Linking ad to ' + regionAttrs.name)
        .pointerClick(pageObject.adSbView.parentInput())
        .setValue(pageObject.adSbView.parentInput(), regionAttrs.name)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .debugLog('Attributes set')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'ad-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'ad-on-map-selected')
        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'ad-on-map-deselected');
}

function createNeutral(attrs) {
    return this
        .createGeoObjectBySuggest('ad_neutral')
        .debugLog('Drawing ad_neutral geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(150, 215)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 215)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 275)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 275)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 215)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Setting ad_neutral attributes...')
        .verifyScreenshot(pageObject.geoObjEditorView(), 'neutral-editor')
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang)
        .debugLog('Linking ad to ' + regionAttrs.name)
        .pointerClick(pageObject.adSbViewCtrl() + ':nth-child(2) input')
        .setValue(pageObject.adSbViewCtrl() + ':nth-child(2) input', regionAttrs.name)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .debugLog('Attributes set')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'neutral-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'neutral-on-map-selected')
        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'neutral-on-map-deselected');
}
