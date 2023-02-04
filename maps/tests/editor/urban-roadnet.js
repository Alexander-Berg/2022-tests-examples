const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT } = require('../../tools/constants'),
    roadnetAttrs = {
        name: 'мост_тест',
        nameLang: 'ru'
    },
    roadnetAttrsChanged = {
        ftType: 'urban_roadnet-ft_type_id__243', // industrial
        name: 'тоннель_тест',
        nameLang: 'en',
        nameType: 'synonym'
    }
;

require('../common.js')(beforeEach, afterEach);

describe('urban roadnet', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn and changed', function() {
        return drawUrbanRoadnet.call(this.browser, roadnetAttrs)
            .verifyScreenshot(pageObject.geoObjViewerView(), 'bridge-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'bridge-on-map-selected')

            .debugLog('Editing urban roadnet...')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .verifyScreenshot(pageObject.geoObjEditorView(), 'bridge-editor')

            .debugLog('Changing ft-type')
            .selectFtType(roadnetAttrsChanged.ftType)

            .debugLog('Changing name')
            .pointerClick(pageObject.textInputControl()) // change urban roadnet name
            .setValue(pageObject.textInputControl(), roadnetAttrsChanged.name)
            .selectNameAttrs(roadnetAttrsChanged.nameLang, roadnetAttrsChanged.nameType)

            .debugLog('Changing urban roadnet geometry...')
            .pause(DRAW_TIMEOUT)
            .leftClick('body', 150, 150)
            .leftClick('body', 150, 150)
            .pause(DRAW_TIMEOUT)
            .verifyMapScreenshot(100, 100, 200, 200, 'tunnel-drawn')

            .saveGeoObject()
            .verifyScreenshot(pageObject.geoObjViewerView(), 'tunnel-viewer')
            .verifyMapScreenshot(100, 100, 200, 200, 'tunnel-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'tunnel-on-map-deselected');
    });
});

function drawUrbanRoadnet(attrs) {
    return this
        .createGeoObject('urban_roadnet_areal')
        .debugLog('Starting to draw a bridge...')

        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'bridge-drawn')

        .debugLog('Setting bridge name...')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang)

        .saveGeoObject();
}
