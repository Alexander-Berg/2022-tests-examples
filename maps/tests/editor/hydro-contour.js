const pageObject = require('../../page-object.js'),
    { DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants'),
    simpleHydroContour = {
        name: 'водоем_автотест'
    },
    complexHydroContour = {
        ftType: 'hydro-ft_type_id__507'
    };

require('../common.js')(beforeEach, afterEach);

describe('hydro-contour', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('-simple is drawn and changed', function() {
        return drawRelief.call(this.browser)
            .then(() => drawSimpleHydro.call(this.browser, simpleHydroContour))
            .then(() => changeSimpleHydro.call(this.browser));
    });

    it('-complex is drawn and changed', function() {
        return drawRelief.call(this.browser)
            .then(() => drawComplexHydro.call(this.browser, complexHydroContour))
            .then(() => changeComplexHydro.call(this.browser));
    });
});

function drawRelief() {
    return this
        .createGeoObject('relief/ft_type_id__302')
        .debugLog('Drawing relief geometry...')
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
        .debugLog('Relief drawn')
        .saveGeoObject()
        .closeSidebar();
}

function drawSimpleHydro(attrs) {
    return this
        .createGeoObject('hydro')
        .debugLog('Drawing hydro geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs()

        .moveToObject('body', 150, 150)
        .pause(DRAW_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-contour-simple-snap')
        .buttonDown()
        .buttonUp()
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 150)
        .pause(UPDATE_TIMEOUT)
        .moveMouseAside()
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-contour-simple-drawn')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'hydro-contour-simple-viewer')
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-contour-simple-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-contour-simple-on-map-deselected');
}

function changeSimpleHydro() {
    return this.pointerClick(200, 250)
        .waitForVisible(pageObject.geoObjViewerView())
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())
        .pointerClick(pageObject.editAddExternalContour())

        .pause(DRAW_TIMEOUT)
        .pointerClick(125, 125)
        .pause(DRAW_TIMEOUT)
        .pointerClick(275, 125)
        .pause(DRAW_TIMEOUT)
        .pointerClick(275, 275)
        .pause(DRAW_TIMEOUT)
        .pointerClick(125, 275)
        .pause(DRAW_TIMEOUT)
        .pointerClick(125, 125)
        .pause(UPDATE_TIMEOUT)
        .moveMouseAside()
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-contour-simple-add-drawn')

        .saveGeoObject()
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-contour-simple-add-on-map-selected')

        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())
        .pointerClick(200, 250)
        .waitForVisible(pageObject.toolbarItem() + ' ' + pageObject.deleteIcon())
        .pointerClick(pageObject.toolbarItem() + ' ' + pageObject.deleteIcon()) // delete internal contour
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-contour-simple-changed-drawn')

        .saveGeoObject()
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-contour-simple-changed-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'hydro-contour-simple-changed-on-map-deselected');
}

function drawComplexHydro(attrs) {
    return this
        .createGeoObject('hydro')
        .selectFtType(attrs.ftType)
        .pointerClick(150, 450)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 450)
        .pause(DRAW_TIMEOUT)
        .moveToObject('body', 150, 150)
        .pause(UPDATE_TIMEOUT)
        .pointerClick(250, 450)
        .pause(UPDATE_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem3()) // add new contour section
        .click(pageObject.ymapsItem3())
        .pointerClick(250, 450)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 550)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 550)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 450)
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 400, 200, 200, 'hydro-contour-complex-drawn')

        .saveGeoObject()
        .verifyMapScreenshot(100, 400, 200, 200, 'hydro-contour-complex-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 400, 200, 200, 'hydro-contour-complex-on-map-deselected');
}

function changeComplexHydro() {
    return this
        .pointerClick(200, 550)
        .waitForVisible(pageObject.geoObjViewerView())
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())
        .waitForInvisible(pageObject.editAddExternalContourDisabled())
        .pointerClick(pageObject.editAddExternalContour())

        // TODO change to 'add internal round_contour' after frontend update

        .pause(DRAW_TIMEOUT)
        .pointerClick(125, 475)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 475)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 525)
        .pause(DRAW_TIMEOUT)
        .pointerClick(125, 525)
        .pause(DRAW_TIMEOUT)
        .pointerClick(125, 475)
        .pause(UPDATE_TIMEOUT)
        .moveMouseAside()
        .verifyMapScreenshot(100, 400, 200, 200, 'hydro-contour-complex-changed-drawn')

        .saveGeoObject()
        .verifyMapScreenshot(100, 400, 200, 200, 'hydro-contour-complex-changed-on-map-selected')

        .closeSidebar()
        .verifyMapScreenshot(100, 400, 200, 200, 'hydro-contour-complex-changed-on-map-deselected');
}
