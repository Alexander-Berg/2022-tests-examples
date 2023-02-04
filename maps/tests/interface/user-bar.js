const pageObject = require('../../page-object.js'),
    { UPDATE_TIMEOUT } = require('../../tools/constants'),
    MAX_ZOOM = 15;

require('../common.js')(beforeEach, afterEach);

describe('user', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it.skip('is logging in', function() {
        return this.browser
            .ensureLoggedIn('common');
    });

    it('login appearance depends on map layer', function() {
        return this.browser
            .setMapCenter([0, 0], MAX_ZOOM)
            .pause(UPDATE_TIMEOUT)
            .verifyScreenshot(pageObject.userBarView(), 'login-on-sat')
            .leftClick(pageObject.mapControlsViewMapTypeImage())
            .pause(UPDATE_TIMEOUT);
            //.verifyScreenshot(pageObject.userBarView(), 'login-on-scheme');
    });

    it('is logging out', function() {
        return this.browser.ensureLogoutFast(true);
    });
});
