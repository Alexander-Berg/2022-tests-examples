const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { DRAW_TIMEOUT, HINT_TIMEOUT, UPDATE_TIMEOUT } = require('../../tools/constants'),
    vegetation1 = {
        ftType: 'vegetation-ft_type_id__221',
        name: 'кладбище_автотест',
        nameLang: 'ru',
        shift: 0,
        selectCoords: [150, 200]
    },
    vegetation2 = {
        ftType: 'vegetation-ft_type_id__403',
        name: 'заповедник_автотест',
        nameLang: 'en',
        shift: 150
    };

require('../common.js')(beforeEach, afterEach);

describe('object with multiple contours', function() {
    beforeEach(function() {
        return this.browser.initNmaps('moderator');
    });

    it('is drawn and deleted', function() {
        const { browser } = this;
        return createVegetation.call(browser, vegetation1)
            .then(() => createVegetation.call(browser, vegetation2))

            .debugLog('Adding slave contour...')
            .pointerClick(pageObject.editIcon())
            .waitForVisible(pageObject.geoObjEditorView())
            .pointerClick(pageObject.editAddExternalContour())
            .waitForVisible(pageObject.contourSelectMode())
            .pointerClick(pageObject.contourSelectMode())
            .waitForNotification('suggest-select-slaves')
            .verifyMapScreenshot(100, 125, 350, 150, 'vegetation-on-map-drawn')
            .moveToObject('body', vegetation1.selectCoords[0], vegetation1.selectCoords[1])
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 125, 350, 150, 'vegetation-add-hint')
            .pointerClick(vegetation1.selectCoords[0], vegetation1.selectCoords[1])
            .pause(UPDATE_TIMEOUT)
            .moveMouseAside()
            .verifyMapScreenshot(100, 125, 350, 150, 'vegetation-changed-drawn')
            .saveGeoObject()

            .moveToObject('body', vegetation1.selectCoords[0], vegetation1.selectCoords[1])
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 125, 500, 150, 'vegetation-on-map-selected-hint')
            .closeSidebar()

            .moveToObject('body', vegetation1.selectCoords[0], vegetation1.selectCoords[1])
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 125, 500, 150, 'vegetation-on-map-deselected-hint')

            .debugLog('Click on common contour')
            .pointerClick(vegetation1.selectCoords[0], vegetation1.selectCoords[1])
            .waitForVisible(pageObject.geoObjViewerView())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'vegetation-two-objects-viewer')

            .debugLog('Deleting second object')
            .pointerClick(pageObject.geoObjRelsViewItem() + '=' + vegetation2.name)
            .waitForVisible(pageObject.moreIcon())
            .pointerClick(pageObject.moreIcon())
            .waitForVisible(pageObject.nkPopup())
            .selectElementByTextInMenu(i18n('common', 'delete'))
            .waitForVisible(pageObject.confirmationView.submit())
            .pointerClick(pageObject.confirmationView.submit())
            .waitForVisible(pageObject.notificationSuccess())
            .waitForVisible(pageObject.geoObjViewerView())
            .verifyScreenshot(pageObject.geoObjViewerView(), 'vegetation-viewer')

            .closeSidebar()
            .setMapCenterByTestNumber()
            .moveToObject('body', vegetation1.selectCoords[0], vegetation1.selectCoords[1])
            .pause(HINT_TIMEOUT)
            .verifyMapScreenshot(100, 125, 350, 150, 'vegetation-one-object-on-map-deselected-hint');
    });
});

function createVegetation(attrs) {
    return this
        .debugLog('Creating vegetation')
        .createGeoObject('vegetation')
        .then(() => drawContour.call(this, attrs.shift))
        .selectFtType(attrs.ftType)
        .pointerClick(pageObject.nkLinkViewAction())
        .waitForVisible(pageObject.textInputControl())
        .pointerClick(pageObject.textInputControl())
        .setValue(pageObject.textInputControl(), attrs.name)
        .selectNameAttrs(attrs.nameLang)
        .saveGeoObject();
}

function drawContour(shift) {
    return this
        .debugLog('Drawing vegetation geometry...')
        .waitForNotification('suggest-draw-slaves')
        .pointerClick(150 + shift, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250 + shift, 150)
        .pause(DRAW_TIMEOUT)
        .pointerClick(250 + shift, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150 + shift, 250)
        .pause(DRAW_TIMEOUT)
        .pointerClick(150 + shift, 150)
        .pause(DRAW_TIMEOUT)
        .debugLog('Geometry drawn');
}
