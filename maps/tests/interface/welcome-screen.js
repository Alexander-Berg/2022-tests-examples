const pageObject = require('../../page-object.js'),
    { INIT_TIMEOUT, INIT_RETRY_COUNT } = require('../../tools/constants'),
    TR_URL = 'https://nmaps.tst.maps.yandex.com.tr/',
    FB_URL = 'https://www.facebook.com/groups/179154909499177/';

require('../common.js')(beforeEach, afterEach);

hermione.skip.in('chrome', 'Welcome screen is not active on TR');

describe('welcome screen', function() {
    beforeEach(function() {
        return this.browser
            .debugLog('Opening nmaps at ' + TR_URL)
            .url(TR_URL)
            .retry(
                () => this.browser.waitForVisible(pageObject.ymaps(), INIT_TIMEOUT),
                INIT_RETRY_COUNT)
            .debugLog('Map visible')
            .clearLocalStorage();
    });

    it('is visible on com.tr and has FB button', function() {
        return this.browser
            .waitForVisible(pageObject.welcomeScreenVideo())
            .waitForVisible(pageObject.welcomeScreenText())
            .waitForVisible(pageObject.welcomeScreenFooter())
            .waitForVisible(pageObject.welcomeScreenFooter.startBtn())
            .waitForVisible(pageObject.welcomeScreenFooter.fbBtn())
            .waitForVisible(pageObject.welcomeScreenClose())
            .verifyScreenshot(pageObject.welcomeScreenContent(), 'welcome-screen')
            .debugLog('Verifying FB url')
            .verifyHrefValue(pageObject.welcomeScreenFooter.fbBtn(), FB_URL);
    });

    it('closes by click on start button', function() {
        return this.browser
            .waitForVisible(pageObject.welcomeScreenContent())
            .waitForVisible(pageObject.welcomeScreenFooter.startBtn())
            .pointerClick(pageObject.welcomeScreenFooter.startBtn())
            .waitForInvisible(pageObject.welcomeScreenContent());
    });

    it('closes by click on close icon', function() {
        return this.browser
            .waitForVisible(pageObject.welcomeScreenContent())
            .waitForVisible(pageObject.welcomeScreenClose())
            .pointerClick(pageObject.welcomeScreenClose())
            .waitForInvisible(pageObject.welcomeScreenContent());
    });

    it('closes by ESC hotkey and doesn\'t appear again on refresh', function() {
        return this.browser
            .waitForVisible(pageObject.welcomeScreenContent())
            .keys('Escape')
            .waitForInvisible(pageObject.welcomeScreenContent())
            .refresh()
            .waitForInvisible(pageObject.welcomeScreenContent());
    });
});
