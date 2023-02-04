const pageObject = require('../../page-object.js'),
    { COMMENTS_TIMEOUT, DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants'),
    condAttrs = {
        name: 'перекрытие_автотест',
        nameLang: 'ru'
    },
    condAttrsChanged = {
        name: 'closure_autotest',
        nameLang: 'fr'
    };

require('../common.js')(beforeEach, afterEach);

describe('cond-closure', function() {
    beforeEach(function() {
        return this.browser.initNmaps('cartographer');
    });

    it('can be created and changed correctly', function() {
        return createRoad.call(this.browser)
            .then(() => createCondClosure.call(this.browser, condAttrs))
            .then(() => editCondClosure.call(this.browser, condAttrsChanged));
    });
});

function createRoad() {
    return this
        .createGeoObject('rd')
        .debugLog('Drawing road geometry...')
        .waitForNotification('suggest-draw-geometry')
        .pointerClick(150, 200)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 200)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 250)
        .pause(DRAW_TIMEOUT)
        .debugLog('Cutting road')
        .pointerClick(250, 200)
        .pause(HINT_TIMEOUT)
        .waitForVisible(pageObject.ymapsItem5()) // cut
        .click(pageObject.ymapsItem5())
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .verifyMapScreenshot(100, 100, 200, 200, 'road-drawn')
        .saveGeoObject();
}

function createCondClosure(attrs) {
    return this
        .createGeoObject('cond_closure')
        .debugLog('Drawing closure geometry...')
        .waitForNotification('suggest-creating-cond')
        .moveToObject('body', 210, 200)
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'closure-before-road-cut')
        .pointerClick(210, 200)
        .waitForNotification('suggest-cond-closure-editor-set-element')
        .verifyMapScreenshot(100, 100, 200, 200, 'closure-after-road-cut')
        .pointerClick(225, 200)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250, 225)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn')
        .moveToObject('body', 300, 300)
        .verifyMapScreenshot(100, 100, 200, 200, 'closure-drawn')

        .debugLog('Setting closure name...')
        .pointerClick(pageObject.fieldsetCtrl1.input())
        .setValue(pageObject.fieldsetCtrl1.input(), attrs.name)
        .selectNameAttrs(attrs.nameLang)
        .debugLog('Name set')
        .pointerClick(100, 100)
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'closure-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'closure-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'closure-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'closure-on-map-selected')
        .closeSidebar()
        .verifyMapScreenshot(100, 100, 200, 200, 'closure-on-map-deselected');
}

function editCondClosure(attrs) {
    return this
        .pointerClick(210, 200)
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pause(COMMENTS_TIMEOUT)
        .waitForVisible(pageObject.geoObjRelsCondClosure())
        .pointerClick(pageObject.geoObjRelsCondClosure())
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pause(COMMENTS_TIMEOUT)

        .debugLog('Editing closure')
        .pointerClick(pageObject.editIcon())
        .waitForVisible(pageObject.geoObjEditorView())

        .debugLog('Changing closure geometry...')
        .pointerClick(pageObject.redrawIcon())
        .waitForNotification('suggest-editing-cond')
        .pointerClick(250, 200)
        .waitForNotification('suggest-cond-closure-editor-set-element')
        .pointerClick(250, 225)
        .pause(DRAW_TIMEOUT)
        .pointerClick(225, 200)
        .pause(UPDATE_TIMEOUT)
        .debugLog('Geometry changed')
        .verifyMapScreenshot(100, 100, 200, 200, 'closure-changed-drawn')

        .debugLog('Changing closure name...')
        .pointerClick(pageObject.fieldsetCtrl1.input())
        .setValue(pageObject.fieldsetCtrl1.input(), attrs.name)
        .selectNameAttrs(attrs.nameLang)
        .debugLog('Name changed')
        .pointerClick(100, 100)
        .waitForInvisible(pageObject.submitDisabled())
        .verifyScreenshot(pageObject.geoObjEditorView(), 'closure-changed-editor')

        .saveGeoObject()
        .verifyScreenshot(pageObject.geoObjViewerView(), 'closure-changed-viewer')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'closure-changed-history')
        .keys(['Backspace'])
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'closure-changed-on-map-selected')

        .debugLog('Verifying highlight on hover')
        .pointerClick(250, 200)
        .waitForVisible(pageObject.geoObjViewerView.commentsLink())
        .pause(COMMENTS_TIMEOUT)
        .waitForVisible(pageObject.geoObjRelsCondClosure())
        .verifyScreenshot(pageObject.geoObjViewerView(), 'junction-viewer')
        .moveToObject(pageObject.geoObjRelsViewItem.link())
        .pause(UPDATE_TIMEOUT)
        .verifyMapScreenshot(100, 100, 200, 200, 'closure-changed-on-map-hover')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), 'junction-history');
}
