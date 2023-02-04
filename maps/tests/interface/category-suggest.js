const pageObject = require('../../page-object.js'),
    { ANIMATION_TIMEOUT, WAIT_FOR_CLASS_TIMEOUT } = require('../../tools/constants'),
    INPUT_WITH_NO_SUGGEST = 'qwerty',
    INPUT_WITH_SUGGEST = 'сад';

require('../common.js')(beforeEach, afterEach);

describe('category suggest input', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is focused onclick create button', function() {
        return this.browser
            .pointerClick(pageObject.appBarView.create())
            .waitForVisible(pageObject.categoryView())
            .waitForExist(pageObject.suggestCategory.textInputFocused(), WAIT_FOR_CLASS_TIMEOUT)
            .verifyScreenshot(pageObject.suggestCategory(), 'input-has-focus');
    });

    it('doesn\'t match anything', function() {
        return this.browser
            .pointerClick(pageObject.appBarView.create())
            .waitForVisible(pageObject.categoryView())
            .pointerClick(pageObject.suggestCategory.input())
            .setValue(pageObject.suggestCategory.input(), INPUT_WITH_NO_SUGGEST)
            .pause(ANIMATION_TIMEOUT)
            .pointerClick(200, 200)
            .pause(ANIMATION_TIMEOUT)
            .verifyScreenshot(pageObject.categoryView.nkIsland(), 'input-has-no-suggest');
    });

    it('has matches', function() {
        return this.browser
            .pointerClick(pageObject.appBarView.create())
            .waitForVisible(pageObject.categoryView())
            .pointerClick(pageObject.suggestCategory.input())
            .setValue(pageObject.suggestCategory.input(), INPUT_WITH_SUGGEST)
            .waitForExist(pageObject.suggestItems())
            .pause(ANIMATION_TIMEOUT)
            .pointerClick(200, 200)
            .verifyScreenshot(pageObject.categoryView.nkIsland(), 'input-has-suggest');
    });
});
