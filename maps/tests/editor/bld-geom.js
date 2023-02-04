const pageObject = require('../../page-object.js');
const { DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');
const bldCoords = {
    firstPoint: [150, 150],
    secondPoint: [250, 150],
    thirdPoint: [247, 250],
    forthPoint: [153, 247]
};
const bldCoordsInner = {
    firstPoint: [175, 175],
    secondPoint: [225, 175],
    thirdPoint: [225, 225],
    forthPoint: [175, 225]
};

require('../common.js')(beforeEach, afterEach);

describe('building', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is drawn with right angle geometry', function() {
        return createBuilding.call(this.browser, bldCoords)
            .verifyMapScreenshot(100, 100, 200, 200, 'building-on-map-selected')
            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-on-map-deselected');
    });

    it('geometry is changed to irregular by redraw icon', function() {
        return createBuilding.call(this.browser, bldCoords)
            .debugLog('Editing building')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing building geometry...')
            .pointerClick(pageObject.redrawIcon())
            .pointerClick(pageObject.snappingToRightGeometry())
            .waitForNotification('suggest-edit-geometry')
            .then(() => drawBuilding.call(this.browser, bldCoords))
            .debugLog('Geometry changed')
            .verifyMapScreenshot(100, 100, 200, 200, 'building-irregular-drawn')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-irregular-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-irregular-on-map-deselected');
    });

    it('geometry is changed to circle by hotkey ctrl+x', function() {
        return createBuilding.call(this.browser, bldCoords)
            .debugLog('Editing building')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing building geometry...')
            .moveToObject('body', 200, 200)
            .keys(['Control', 'x'])
            .pause(DRAW_TIMEOUT)
            .pointerClick(pageObject.circle())
            .pause(DRAW_TIMEOUT)
            .mouseDrag([200, 200], [250, 250])
            .pause(UPDATE_TIMEOUT)
            .debugLog('Geometry changed')
            .verifyMapScreenshot(100, 100, 200, 200, 'building-circle-drawn', { tolerance: 95 })

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-circle-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-circle-on-map-deselected', { tolerance: 95 });
    });

    it('geometry is changed by drag', function() {
        return createBuilding.call(this.browser, bldCoords)
            .debugLog('Editing building')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing building geometry...')
            .moveToObject('body', 200, 150)
            .pause(DRAW_TIMEOUT)
            .mouseDrag([200, 150], [200, 175])
            .pause(UPDATE_TIMEOUT)
            .debugLog('Geometry changed')
            .verifyMapScreenshot(100, 100, 200, 200, 'building-drag-drawn')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-drag-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-drag-on-map-deselected');
    });

    it('geometry is split', function() {
        return createBuilding.call(this.browser, bldCoords)
            .debugLog('Editing building')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing building geometry...')
            .waitForNotification('suggest-edit-geometry')
            .pointerClick(pageObject.splitIcon())
            .waitForNotification('suggest-split-geometry')
            .pointerClick(150, 150)
            .pause(DRAW_TIMEOUT)
            .pointerClick(200, 250)
            .pause(DRAW_TIMEOUT)
            .pointerClick(275, 125)
            .pause(UPDATE_TIMEOUT)
            .debugLog('Geometry changed')
            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-split-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-split-on-map-deselected');
    });

    it('has internal contour', function() {
        return createBuilding.call(this.browser, bldCoords)
            .debugLog('Editing building')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing building geometry...')
            .waitForNotification('suggest-edit-geometry')
            .pointerClick(250, 150)
            .pause(DRAW_TIMEOUT)
            .waitForVisible(pageObject.ymapsItem2()) // add internal contour
            .pointerClick(pageObject.ymapsItem2())
            .waitForNotification('suggest-edit-geometry')
            .then(() => drawBuilding.call(this.browser, bldCoordsInner))
            .debugLog('Geometry changed')

            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-internal-on-map-selected')

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-internal-on-map-deselected');
    });

    it('geometry is scaled and rotated', function() {
        return createBuilding.call(this.browser, bldCoords)
            .debugLog('Editing building')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())

            .debugLog('Changing building geometry...')
            .pointerClick(pageObject.framing())
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', 250, 150)
            .pause(DRAW_TIMEOUT)
            .mouseDrag([200, 200], [220, 180])
            .pause(DRAW_TIMEOUT)
            .moveToObject('body', 220, 100)
            .pause(DRAW_TIMEOUT)
            .mouseDrag([220, 100], [270, 130], 1000)
            .pause(UPDATE_TIMEOUT)
            .debugLog('Geometry changed')
            .moveMouseAside()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-scaled-drawn', { tolerance: 95 })
            .saveGeoObject()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-scaled-on-map-selected', { tolerance: 75 })

            .closeSidebar()
            .verifyMapScreenshot(100, 100, 200, 200, 'building-scaled-on-map-deselected', { tolerance: 75 });
    });
});

function createBuilding(coords) {
    return this
        .createGeoObject('bld')
        .waitForNotification('suggest-draw-geometry')
        .then(() => drawBuilding.call(this, coords))
        .saveGeoObject();
}

function drawBuilding(coords) {
    return this
        .debugLog('Drawing building geometry...')
        .pointerClick(coords.firstPoint[0], coords.firstPoint[1])
        .pause(DRAW_TIMEOUT)
        .pointerClick(coords.secondPoint[0], coords.secondPoint[1])
        .pause(DRAW_TIMEOUT)
        .pointerClick(coords.thirdPoint[0], coords.thirdPoint[1])
        .pause(DRAW_TIMEOUT)
        .pointerClick(coords.forthPoint[0], coords.forthPoint[1])
        .pause(UPDATE_TIMEOUT)
        .debugLog('Geometry drawn');
}
