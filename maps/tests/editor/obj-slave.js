const pageObject = require('../../page-object.js');
const i18n = require('../../tools/i18n.js');
const { DRAW_TIMEOUT, UPDATE_TIMEOUT, SUGGEST_TIMEOUT } = require('../../tools/constants');
const MAX_ZOOM = 22;
const RAILWAY_LINE_NAME = 'ж/д_тестинг';
const firstRailwayElCoords = {
    start: [150, 150],
    end: [250, 150]
};
const secondRailwayElCoords = {
    start: [150, 250],
    end: [250, 250]
};

require('../common.js')(beforeEach, afterEach);

describe('slave object', function() {
    beforeEach(function() {
        return this
            .browser.initNmaps('moderator')
            .setMapCenterByTestNumber(MAX_ZOOM);
    });

    it('is highlighted correctly', function() {
        return createRailwayEl.call(this.browser, firstRailwayElCoords)
            .then(() => createRailwayEl.call(this.browser, secondRailwayElCoords))
            .verifyMapScreenshot(100, 100, 200, 200, '2nd-railway-el-linked-on-map-selected-1')
            .then(() => deleteLinkToRailwayLine.call(this.browser))
            .verifyMapScreenshot(100, 100, 200, 200, '2nd-railway-el-unlinked-on-map-selected', { tolerance: 75 })
            .then(() => linkToRailwayLine.call(this.browser, RAILWAY_LINE_NAME, true))
            .verifyMapScreenshot(100, 100, 200, 200, '2nd-railway-el-linked-on-map-selected-2')
            .then(() => deleteRailwayEl.call(this.browser))
            .verifyMapScreenshot(100, 100, 200, 200, '2nd-railway-el-deleted-on-map-invisible', { tolerance: 75 })
            .then(() => revertRailwayElCommit.call(this.browser, 3))
            .verifyMapScreenshot(100, 100, 200, 200, '2nd-railway-el-linked-on-map-selected-3')
            .waitForVisible(pageObject.editIcon())
            .then(() => deleteRailwayEl.call(this.browser))
            .pointerClick(200, 150)
            .waitForVisible(pageObject.geoObjViewerView())
            .moveMouseAside()
            .verifyMapScreenshot(100, 100, 200, 200, '1st-railway-el-linked-on-map-selected');
    });
});

function createRailwayEl(coords) {
    return this
        .createGeoObject('transport_railway_el')
        .debugLog('Drawing railway_el geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(coords.start[0], coords.start[1])
        .pause(DRAW_TIMEOUT)
        .pointerClick(coords.end[0], coords.end[1])
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .then(() => linkToRailwayLine.call(this, RAILWAY_LINE_NAME));
}

function linkToRailwayLine(name, isSaved) {
    return this
        .debugLog('Linking railway_el to line ' + name)
        .then(() => isSaved?
            this.pointerClick(pageObject.editIcon())
                .waitForVisible(pageObject.geoObjEditorView()) :
            true
        )
        .moveMouseAside()
        .pointerClick(pageObject.geoObjMastersEditorViewSuggest.input())
        .setValue(pageObject.geoObjMastersEditorViewSuggest.input(), name)
        .waitForVisible(pageObject.suggestPopup())
        .pause(SUGGEST_TIMEOUT)
        .keys(['ArrowDown', 'Enter'])
        .waitForInvisible(pageObject.suggestPopup())
        .pause(UPDATE_TIMEOUT)
        .saveGeoObject();
}

function deleteLinkToRailwayLine() {
    return this
        .debugLog('Deleting assignment to railway line')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())
        .waitForVisible(pageObject.deleteIconSmall())
        .pointerClick(pageObject.deleteIconSmall())
        .saveGeoObject();
}

function deleteRailwayEl() {
    return this
        .debugLog('Deleting railway_el...')
        .pointerClick(pageObject.moreIcon())
        .waitForVisible(pageObject.nkPopup())
        .selectElementByTextInMenu(i18n('common', 'delete'))
        .waitForVisible(pageObject.sidebarView() + ' ' + pageObject.confirmationView.submit())
        .pause(UPDATE_TIMEOUT)
        .pointerClick(pageObject.sidebarView() + ' ' + pageObject.confirmationView.submit())
        .waitForInvisible(pageObject.sidebarView() + ' ' + pageObject.confirmationView())
        .waitForNotification('geoobject-deleted')
        .debugLog('Railway_el deleted')
        .setMapCenterByTestNumber(MAX_ZOOM)
        .pause(UPDATE_TIMEOUT);
}

function revertRailwayElCommit(index) {
    return this
        .debugLog('Proceeding to deleted railway_el viewer')
        .pointerClick(200, 150)
        .waitForVisible(pageObject.geoObjRelsViewItem.link())
        .pointerClick(pageObject.geoObjRelsViewItem.link())
        .waitForVisible(pageObject.geoObjViewerView())
        .waitForVisible(pageObject.geoObjViewerView.historyLink())
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .pointerClick(pageObject.listItemView() + ':nth-child(1)')
        .waitForVisible(pageObject.commitDiffViewRelatedGeoObjLink())
        .pointerClick(pageObject.commitDiffViewRelatedGeoObjLink())
        .waitForVisible(pageObject.geoObjViewerView.historyLink())
        .verifyScreenshot(pageObject.geoObjViewerView(), '2nd-railway-el-deleted-viewer', { ignore: 'viewer-date'})

        .debugLog('Reverting railway_el #' + index + ' commit...')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .pointerClick(pageObject.sidebarView() + ' ' + pageObject.listItemView() + ':nth-child(' + index + ')')
        .waitForVisible(pageObject.sidebarView() + ' ' + pageObject.listItemExpanded())
        .pointerClick(pageObject.sidebarView() + ' ' + pageObject.nkButton())
        .waitForVisible(pageObject.confirmationView.submit())
        .pointerClick(pageObject.confirmationView.submit())
        .waitForNotification('commit-reverted')
        .debugLog('Commit reverted')
        .setMapCenterByTestNumber(MAX_ZOOM)
        .pause(UPDATE_TIMEOUT);
}
