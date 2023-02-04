const pageObject = require('../../page-object.js');
const HARITA_URL = /(\/\/yandex.com.tr\/harita\/)(.*?)/;

require('../common.js')(beforeEach, afterEach);

describe('on com.tr', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common', 'com.tr');
    });

    it('mrc and fb icons are not visible', function() {
        return this.browser
            .setMapCenter([37.583898, 55.752368])
            .waitForVisible(pageObject.monitorIcon())
            .waitForVisible(pageObject.mrcIcon(), null, true)
            .waitForVisible(pageObject.panoramaIcon())
            .waitForVisible(pageObject.ymapsIcon())
            .waitForVisible(pageObject.feedbackIcon(), null, true);
    });

    it('ymaps icon should lead to harita', function() {
        return this.browser
            .waitForVisible(pageObject.ymapsIcon())
            .getAttribute(pageObject.ymapsIconBtn(), 'href')
            .then((hrefUrl) => {
                if (hrefUrl.match(HARITA_URL)) {
                    return this.browser.debugLog(hrefUrl + ' matches expected url. OK!')
                } else {
                    throw new Error(hrefUrl + ' doesn\'t match expected url');
                }
                return true;
            });
    });

    it('impossible to change lang', function() {
        return this.browser
            .waitForVisible(pageObject.userIcon())
            .pointerClick(pageObject.userIcon())
            .waitForVisible(pageObject.nkPopup.menu())
            .waitForVisible(pageObject.userFlag(), null, true)
            .verifyScreenshot(pageObject.nkPopup.menu(), 'tr-user-menu');
    });

    it('no tracker layer', function() {
        return this.browser
            .waitForVisible(pageObject.layerManagerViewLayers())
            .pointerClick(pageObject.layerManagerViewLayers())
            .waitForVisible(pageObject.layerManagerViewAdditionalLayersMenu())
            .waitForVisible(pageObject.layerTracker(), null, true)
            .verifyScreenshot(pageObject.layerManagerViewAdditionalLayersMenu(),
                'tr-additional-layers-popup-content');
    });
});
