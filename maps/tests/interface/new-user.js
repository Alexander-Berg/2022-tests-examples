const pageObject = require('../../page-object.js'),
    { UPDATE_TIMEOUT } = require('../../tools/constants'),
    i18n = require('../../tools/i18n.js');

require('../common.js')(beforeEach, afterEach);

hermione.skip.in('chrome', 'Need to modify');

describe('novice', function() {
    const editProfile = i18n('user-profile', 'edit-profile'),
        newsSubscription = i18n('user-profile', 'news-subscription');

    beforeEach(function() {
        return this.browser
            .initNmaps('newbie')
            .clearLocalStorage()
            .resetUser()
            .initNmaps('newbie');
    });

    it('has welcome screen visible', function() {
        return this.browser.waitForVisible(pageObject.welcomeScreen());
    });

    it('sees 5 hardcoded layers', function() {
        return this.browser
            .pointerClick(pageObject.layerManagerViewLayers())
            .waitForVisible(pageObject.layerManagerViewLayersMenu())
            .pause(UPDATE_TIMEOUT)
            .verifyScreenshot(pageObject.layerManagerViewLayersMenu(), 'layers-popup-content');
    });

    it('has subscription alert', function() {
        return this.browser
            .pointerClick(pageObject.userIcon())
            .waitForVisible(pageObject.userProfileLink())
            .pointerClick(pageObject.userProfileLink())
            .waitForVisible(pageObject.nkButton() + '=' + editProfile)
            .pointerClick(pageObject.nkButton() + '=' + editProfile)
            .waitForVisible(pageObject.nkCheckbox() + '=' + newsSubscription)
            .blurInput(pageObject.textareaControl())
            .verifyScreenshot(pageObject.sidebarView(), 'user-profile-editor-news-subscription-off')
            .ensureLogoutFast() // reload page 3 times to escape welcome screen
            .ensureLoggedInFast('newbie')
            .prepareNmaps()
            .ensureLogoutFast()
            .ensureLoggedInFast('newbie')
            .prepareNmaps()
            .ensureLogoutFast()
            .ensureLoggedInFast('newbie')
            .prepareNmaps()
            .waitForVisible(pageObject.subscriptionAlert())
            .verifyMapScreenshot(390, 380, 500, 400, 'subscription-alert');
    });
});
