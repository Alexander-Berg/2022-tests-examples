const pageObject = require('../../page-object.js');

require('../common.js')(beforeEach, afterEach);

describe('create button', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is visible', function() {
        return this.browser.waitForVisible(pageObject.appBarView.create());
    });

    it('shows category selector', function() {
        return this.browser
            .waitForVisible(pageObject.appBarView.create())
            .pointerClick(pageObject.appBarView.create())
            .waitForVisible(pageObject.categoryView())
            .verifyScreenshot(pageObject.categoryView.nkIsland(), 'category-selector');
    });
});
