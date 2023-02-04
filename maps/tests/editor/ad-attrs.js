const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT, SUGGEST_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants'),
    ad = {
        name: 'еатд_тестинг',
        linkTo: 'город Париж',
        nameLang: 'tr',
        population: '12345'
    },
    adChanged = {
        name: 'район_тестинг',
        nameType: 'short',
        nameLang: 'en',
        ftType: 'ad-level_kind__5',
        linkTo: 'посёлок Пудость',
        population: '123'
    };

require('../common.js')(beforeEach, afterEach);

describe('ad', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn by default', function() {
        return createAd.call(this.browser, ad)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'ad-default-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-default-on-map-selected')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-default-on-map-deselected');
    });

    it('is drawn with attributes and changed', function() {
        return createAd.call(this.browser, ad, true)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'ad-with-attrs-viewer')
            .then(() => editAd.call(this.browser, adChanged))
            .verifyScreenshot(pageObject.geoObjViewerView(), 'ad-changed-viewer')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'ad-changed-on-map-deselected');
    });
});

function createAd(obj, isFull) {
    return this
        .createGeoObject('ad')
        .debugLog('Drawing ad geometry...')
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

        .debugLog('Setting ad name as ' + obj.name)
        .pointerClick(pageObject.fieldsetCtrl1.input())
        .setValue(pageObject.fieldsetCtrl1.input(), obj.name)
        .selectNameAttrs(obj.nameLang)

        .then(() => isFull? this
            .debugLog('Linking ad to ' + obj.linkTo)
                .pointerClick(pageObject.adSbView.parentInput())
                .setValue(pageObject.adSbView.parentInput(), obj.linkTo)
                .waitForVisible(pageObject.suggestPopup())
                .pause(SUGGEST_TIMEOUT)
                .keys(['ArrowDown', 'Enter'])
                .waitForInvisible(pageObject.suggestPopup())

                .debugLog('Setting ad as a center of region')
                .pointerClick(pageObject.adSbView.capital())
                .waitForVisible(pageObject.nkPopup())
                .selectElementByTextInMenu(i18n('attr-values', 'ad-capital__2'))

                .debugLog('Checking city checkbox')
                .pointerClick(pageObject.adSbView.city())
                .debugLog('Setting ad population')
                .pointerClick(pageObject.adSbView.populationInput())
                .setValue(pageObject.adSbView.populationInput(), obj.population) :
            true
        )
        .saveGeoObject();
}

function editAd(obj) {
    return this
        .debugLog('Editing ad attrs')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing ad ft_type')
        .selectFtType(obj.ftType)

        .debugLog('Changing ad name to ' + obj.name)
        .pointerClick(pageObject.fieldsetCtrl1.input())
        .setValue(pageObject.fieldsetCtrl1.input(), obj.name)
        .selectNameAttrs(obj.nameLang, obj.nameType)

        .debugLog('Changing ad link to ' + obj.linkTo)
        .pointerClick(pageObject.adSbView.parentInput())
        .pause(SUGGEST_TIMEOUT)
        .pointerClick(pageObject.adSbView.parentInputClear())
        .pointerClick(pageObject.adSbView.parentInput())
        .setValue(pageObject.adSbView.parentInput(), obj.linkTo)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())

        .debugLog('Setting ad as a center of district')
        .pointerClick(pageObject.adSbView.capital())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('attr-values', 'ad-capital__3'))
        .debugLog('Unchecking city checkbox')
        .pointerClick(pageObject.adSbView.city())
        .debugLog('Checking municipality checkbox')
        .pointerClick(pageObject.adSbView.municipality())
        .debugLog('Checking not official checkbox')
        .pointerClick(pageObject.adSbView.notOfficial())
        .debugLog('Changing ad population')
        .pointerClick(pageObject.adSbView.populationInput())
        .setValue(pageObject.adSbView.populationInput(), obj.population)
        .blurInput(pageObject.adSbView.populationInput())
        .pause(UPDATE_TIMEOUT)

        .verifyScreenshot(pageObject.geoObjEditorView(), 'ad-changed-editor')
        .saveGeoObject();
}
