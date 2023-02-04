const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants'),
    territory = {
        linkedPoiName: 'пои_автотест',
        ftType: 'urban_areal-ft_type_id__173' // trade
    };

require('../common.js')(beforeEach, afterEach);

describe('territory', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and linked', function() {
        return drawTerritory.call(this.browser, territory)
            .debugLog('Selecting territory')
            .pointerClick(175, 175)
            .waitForVisible(pageObject.geoObjViewerView())
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Verifying help url')
            .verifyHrefValue(pageObject.sidebarHeader() + ' ' + pageObject.nkLink(),
                'https:' + i18n('categories-help-urls', 'urban_areal'))

            .debugLog('Attaching master organization')
            .pointerClick(pageObject.geoObjMasterEditorViewSuggest.input())
            .waitForVisible(pageObject.suggestPopup())
            .pause(SUGGEST_TIMEOUT)
            .keys(['ArrowDown', 'ArrowDown', 'Enter']) // link to poi without name
            .waitForInvisible(pageObject.suggestPopup())
            .pause(UPDATE_TIMEOUT)
            .blurInput(pageObject.geoObjMasterEditorViewSuggest.input())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'territory-linked-editor')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'territory-linked-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'territory-linked-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'territory-linked-on-map-deselected')

            .moveToObject('body', 175, 175)
            .pause(UPDATE_TIMEOUT)
            .verifyMapScreenshot(100, 100, 250, 200, 'territory-linked-hover');
    });
});

function drawPois() {
    return this
        .createGeoObject('poi_auto')
        .debugLog('Drawing first point')
        .pause(DRAW_TIMEOUT)
        .pointerClick(200, 175)
        .pause(DRAW_TIMEOUT)

        .debugLog('Adding POI rubric')
        .pointerClick(pageObject.poiSbView.rubricInput())
        .waitForVisible(pageObject.suggestPopup())
        .pointerClick(pageObject.suggestPopup.item() + ':nth-child(3)')

        .saveGeoObject()

        .debugLog('Drawing second point')
        .pointerClick(pageObject.sidebarView.island2() + ' ' +
            pageObject.listItemView() + ':nth-child(1)') // add next one automoto
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Adding second point name')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.listCtrl() + ' ' + pageObject.nkTextInputControl())
        .setValue(pageObject.listCtrl() + ' ' + pageObject.nkTextInputControl(), territory.linkedPoiName)

        .debugLog('Adding second point geometry')
        .pause(DRAW_TIMEOUT)
        .pointerClick(200, 225)
        .pause(DRAW_TIMEOUT)

        .saveGeoObject()
        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'pois-on-map-deselected');
}

function drawTerritory(attrs) {
    return drawPois.call(this)
        .createGeoObject('urban')

        .debugLog('Setting territory ft_type')
        .selectFtType(attrs.ftType)

        .debugLog('Drawing territory')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(175, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(175, 250)
        .pause(UPDATE_TIMEOUT)
        .pointerClick(175, 250)
        .pause(HINT_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem1()) // finish
        .click(pageObject.ymapsItem1())
        .pause(DRAW_TIMEOUT)
        .verifyScreenshot(pageObject.geoObjEditorView(), 'territory-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'territory-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'territory-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'territory-on-map-deselected');
}
