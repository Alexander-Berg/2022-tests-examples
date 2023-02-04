const pageObject = require('../../page-object.js');
const { DRAW_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('on com.tr', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common', 'com.tr');
    });

    it('objects can be created', function() {
        return createBuilding.call(this.browser)
            .then(() => revertBuilding.call(this.browser))
    });

    it('mosque is default poi_religion ft_type', function() {
        return this.browser
            .createGeoObject('poi_religion')
            .waitForNotification('suggest-drawing-poi-geometry', 'tr')
            .verifyScreenshot(pageObject.geoObjEditorView(), 'tr-poi-religion-editor');
    });

    it('cond-cam not available', function() {
        return this.browser
            .pointerClick(pageObject.appBarView.create())
            .waitForVisible(pageObject.categorySelectorGroupsView.group() + ':nth-child(11)') // urban_roadnet_group
            .pointerClick(pageObject.categorySelectorGroupsView.group() + ':nth-child(11)')
            .waitForVisible(pageObject.listItemGroup())
            .verifyScreenshot(pageObject.listItemGroup(), 'tr-no-camera-list');
    });
});

function createBuilding() {
    return this
        .createGeoObject('bld')

        .debugLog('Drawing bld geometry...')
        .waitForNotification('suggest-draw-geometry', 'tr')
        .pointerClick(150, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'tr-bld-viewer');
}

function revertBuilding() {
    return this
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .pointerClick(pageObject.commitView())
        .waitForVisible(pageObject.commitView.button())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'tr-bld-history')
        .pointerClick(pageObject.commitView.button())
        .waitForVisible(pageObject.geoObjViewerView.deleted())
        .waitForVisible(pageObject.notificationSuccess())
        .debugLog('Bld reverted');
}
