const pageObject = require('../../page-object.js'),
    { ANIMATION_TIMEOUT } = require('../../tools/constants'),
    i18n = require('../../tools/i18n.js');

require('../common.js')(beforeEach, afterEach);

hermione.skip.in('chrome', 'Need to update this autotest');

describe('help panel', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('is visible for common user', function() {
        return this.browser
            .pointerClick(pageObject.helpIcon())
            .waitForVisible(pageObject.helpView())
            .waitForVisible(pageObject.helpView.header())
            .waitForVisible(pageObject.helpViewOnboarding())
            .waitForVisible(pageObject.helpViewVideo())
            .waitForVisible(pageObject.helpViewLive())
            .waitForVisible(pageObject.helpViewPrimaryLinks())
            .waitForVisible(pageObject.helpViewAdditionalLinks())
            .waitForVisible(pageObject.helpView.close())
            .debugLog('Verifying club url')
            .verifyHrefValue(pageObject.helpViewPrimaryLinks.club(), i18n('help', 'club-url'))
            .debugLog('Verifying VK url')
            .verifyHrefValue(pageObject.helpViewPrimaryLinks.vk(), i18n('help', 'vkontakte-url'))
            .debugLog('Verifying rules url')
            .verifyHrefValue(pageObject.helpViewPrimaryLinks.rules(), 'https:' + i18n('help', 'rules-url'))
            .debugLog('Verifying agreement url')
            .verifyHrefValue(pageObject.helpViewAdditionalLinks.agreement(), i18n('help', 'user-agreement-url'))
            .pause(ANIMATION_TIMEOUT)
            .verifyScreenshot(pageObject.helpView(), 'help-view');
    });

    it('is special for moderator', function() {
        return this.browser
            .ensureLoggedInFast('moderator')
            .waitForVisible(pageObject.helpIcon())
            .pointerClick(pageObject.helpIcon())
            .waitForVisible(pageObject.helpView())
            .debugLog('Verifying moderator club url')
            .verifyHrefValue(pageObject.helpViewPrimaryLinks.modClub(), i18n('help', 'moderator-club-url'))
            .debugLog('Verifying rules changes url')
            .verifyHrefValue(pageObject.helpViewPrimaryLinks.rulesChanges(), 'https:' + i18n('help', 'rules-changes-url'))
            .pause(ANIMATION_TIMEOUT)
            .verifyScreenshot(pageObject.helpView(), 'help-view-moderator');
    });

    it('disappears after 3 page reloads', function() {
        let browser = this.browser;
        return browser
            .isVisible(pageObject.helpView())
            .then(isVisible => isVisible && browser.pointerClick(pageObject.helpView.close()))
            .then(() => {
                for(let i = 1; i <= 3; i++) {
                    browser = browser
                        .debugLog(i + ' reload - should be visible')
                        .refresh()
                        .waitForVisible(pageObject.helpView())
                        .pointerClick(pageObject.helpView.close());
                }
                return browser;
            })
            .debugLog('one more reload - should be invisible')
            .refresh()
            .waitForVisible(pageObject.appBarView.create())
            .waitForInvisible(pageObject.helpView());
    });

    it('has link and opens video player', function() {
        return this.browser
            .pointerClick(pageObject.helpIcon())
            .waitForVisible(pageObject.helpView())
            .waitForVisible(pageObject.helpViewVideo())
            .pointerClick(pageObject.helpViewVideo() + ' ' + pageObject.nkButton())
            .waitForVisible(pageObject.helpViewVideoPlayer())
            .pointerClick(pageObject.helpViewVideoPlayerClose())
            .waitForInvisible(pageObject.helpViewVideoPlayer())
            .pointerClick(pageObject.helpViewVideoPreview())
            .waitForVisible(pageObject.helpViewVideoPlayer())
            .keys('Escape')
            .waitForInvisible(pageObject.helpViewVideoPlayer());
    });

    it('closes by ESC hotkey', function() {
        return this.browser
            .pointerClick(pageObject.helpIcon())
            .waitForVisible(pageObject.helpView())
            .keys('Escape')
            .waitForInvisible(pageObject.helpView());
    });

    it('closes by close button click', function() {
        return this.browser
            .pointerClick(pageObject.helpIcon())
            .waitForVisible(pageObject.helpView())
            .pointerClick(pageObject.helpView.close())
            .waitForInvisible(pageObject.helpView());
    });
});
