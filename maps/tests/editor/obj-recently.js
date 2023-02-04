const pageObject = require('../../page-object.js');
const { DRAW_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('recently objects', function() {
    beforeEach(function() {
        return this.browser.initNmaps('moderator');
    });

    it('panel shows 3 last created categories', function() {
        return createRoad.call(this.browser)
            .then(() => createBuilding.call(this.browser))
            .then(() => createPoi.call(this.browser))
            .then(() => createVegetation.call(this.browser))
            .then(() => createReliefPoint.call(this.browser))
            .then(() => createRiver.call(this.browser))
            .then(() => createTerritory.call(this.browser))
            .pointerClick(pageObject.appBarView.create())
            .waitForVisible(pageObject.categoryView.lastUsed())
            .verifyScreenshot(pageObject.categoryView.lastUsed(), 'recently-created-objects-panel');
    });
});

function createRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .saveGeoObject()
        .verifyMapScreenshot(100, 100, 200, 200, 'road-on-map-selected');
}

function createBuilding() {
    return this
        .createGeoObject('bld')
        .debugLog('Drawing building geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(125, 175)
        .pause(DRAW_TIMEOUT)
        .pointerClick(185, 175)
        .pause(DRAW_TIMEOUT)
        .pointerClick(155, 225)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .saveGeoObject()
        .verifyMapScreenshot(100, 100, 200, 200, 'building-on-map-selected');
}

function createPoi() {
    return this
        .createGeoObject('poi_sport')
        .waitForNotification('suggest-drawing-poi-geometry')
        .pointerClick(200, 200)
        .pause(DRAW_TIMEOUT)

        .debugLog('Adding poi rubric')
        .pointerClick(pageObject.poiSbView.rubricInput())
        .waitForVisible(pageObject.suggestPopup())
        .pointerClick(pageObject.suggestPopup.item() + ':nth-child(2)')

        .saveGeoObject()
        .verifyMapScreenshot(100, 100, 200, 200, 'poi-on-map-selected');
}

function createVegetation() {
    return this
        .createGeoObject('vegetation')
        .debugLog('Drawing vegetation geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(215, 175)
        .pause(DRAW_TIMEOUT)
        .pointerClick(275, 175)
        .pause(DRAW_TIMEOUT)
        .pointerClick(245, 225)
        .pause(DRAW_TIMEOUT)
        .pointerClick(215, 175)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .saveGeoObject()
        .verifyMapScreenshot(100, 100, 200, 200, 'vegetation-on-map-selected');
}

function createReliefPoint() {
    return this
        .createGeoObject('relief_point/ft_type_id__308') // volcano
        .waitForNotification('suggest-draw-point')
        .pointerClick(245, 200)
        .pause(DRAW_TIMEOUT)

        .saveGeoObject()
        .verifyMapScreenshot(100, 100, 200, 200, 'relief-on-map-selected');
}

function createRiver() {
    return this
        .createGeoObject('hydro_ln_el')
        .debugLog('Drawing river geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .saveGeoObject()
        .verifyMapScreenshot(100, 100, 200, 200, 'river-on-map-selected');
}

function createTerritory() {
    return this
        .createGeoObject('urban')
        .debugLog('Drawing territory geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(200, 175)
        .pause(DRAW_TIMEOUT)
        .pointerClick(180, 225)
        .pause(DRAW_TIMEOUT)
        .pointerClick(220, 225)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .debugLog('Linking territory to poi...')
        .pointerClick(pageObject.geoObjMasterEditorViewSuggest.input())
        .waitForVisible(pageObject.suggestPopup())
        .keys(['ArrowDown', 'ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .pause(UPDATE_TIMEOUT)

        .saveGeoObject()
        .verifyMapScreenshot(100, 100, 200, 200, 'territory-on-map-selected', { tolerance: 95 })
        .verifyScreenshot(pageObject.geoObjViewerView(), 'territory-viewer');
}
