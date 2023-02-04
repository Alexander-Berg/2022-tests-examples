const pageObject = require('../../page-object.js'),
    { WAIT_FOR_CLASS_TIMEOUT } = require('../../tools/constants');

require('../common.js')(beforeEach, afterEach);

describe('in bottom controls', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('all icons should be visible', function() {
        return this.browser
            .waitForVisible(pageObject.monitorIcon())
            .waitForVisible(pageObject.mrcIcon())
            .waitForVisible(pageObject.panoramaIcon())
            .waitForVisible(pageObject.ymapsIcon());
    });

    it('panorama icon should be disabled in span with no panoramas', function() {
        return this.browser
            .setMapCenter([37.58978, 55.73335])
            .waitForExist(pageObject.panoramaIconDisabled(), WAIT_FOR_CLASS_TIMEOUT, true)
            .setMapCenter([0, 0])
            .waitForExist(pageObject.panoramaIconDisabled(), WAIT_FOR_CLASS_TIMEOUT);
    });
});
