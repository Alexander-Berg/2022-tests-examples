const pageObject = require('../../page-object.js'),
    { UPDATE_TIMEOUT } = require('../../tools/constants'),
    i18n = require('../../tools/i18n.js'),
    MAX_ZOOM = 16;

require('../common.js')(beforeEach, afterEach);

describe('layers', function() {
    const select = i18n('layers-controls', 'select-all-layers'),
        deselect = i18n('layers-controls', 'deselect-all-layers');

    beforeEach(function() {
        return this.browser
            .initNmaps('common')
            .clearLocalStorage()
            .setMapCenter([0, 0], MAX_ZOOM);
    });

    hermione.skip.in('firefox');

    it('control should open layers popup content', function() {
        const { browser } = this;

        return browser
            .pointerClick(pageObject.layerManagerViewLayers())
            .waitForVisible(pageObject.layerManagerViewLayersMenu())
            .isVisible(pageObject.button() + '=' + deselect)
            .then(isVisible => isVisible && browser.pointerClick(pageObject.button() + '=' + deselect))
            .verifyScreenshot(pageObject.layerManagerView(), 'layers-icon-off-1')
            .verifyScreenshot(pageObject.layerManagerViewLayersMenu(), 'layers-popup-content-all-off')
            .pointerClick(pageObject.button() + '=' + select)
            .verifyScreenshot(pageObject.layerManagerView(), 'layers-icon-on')
            .verifyScreenshot(pageObject.layerManagerViewLayersMenu(), 'layers-popup-content-all-on')
            .pointerClick(pageObject.layerManagerViewLayersMenu.item5())
            .verifyScreenshot(pageObject.layerManagerViewLayersMenu(), 'layers-popup-content-one-off',
                { notMoveMouse: true })
            .verifyScreenshot(pageObject.layerManagerView(), 'layers-icon-off-2');
    });

    it('should persist upon refresh', function() {
        const { browser } = this;

        return browser
            .pointerClick(pageObject.layerManagerViewLayers())
            .waitForVisible(pageObject.layerManagerViewLayersMenu())
            .isVisible(pageObject.button() + '=' + deselect)
            .then(isVisible => isVisible && browser.pointerClick(pageObject.button() + '=' + deselect))
            .pointerClick(pageObject.layerManagerViewLayersMenu.item1())
            .verifyScreenshot(pageObject.layerManagerViewLayersMenu(), 'persistent-configuration-1')
            .refresh()
            .waitForVisible(pageObject.ymaps())
            .pause(UPDATE_TIMEOUT)
            .waitForVisible(pageObject.layerManagerViewLayers())
            .pointerClick(pageObject.layerManagerViewLayers())
            .waitForVisible(pageObject.layerManagerViewLayersMenu())
            .verifyScreenshot(pageObject.layerManagerViewLayersMenu(), 'persistent-configuration-2');
    });

    it('map type control should change on click', function() {
        return this.browser
            .pause(UPDATE_TIMEOUT)
            .verifyScreenshot(pageObject.mapControlsViewMapTypeImage(), 'small-map-with-scheme')
            .pointerClick(pageObject.mapControlsViewMapTypeImage())
            .pause(UPDATE_TIMEOUT)
            .verifyScreenshot(pageObject.mapControlsViewMapTypeImage(), 'small-map-with-sat');
    });
});
