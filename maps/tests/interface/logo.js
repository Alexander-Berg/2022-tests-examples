const pageObject = require('../../page-object.js');

require('../common.js')(beforeEach, afterEach);

describe('logo', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('should be visible', function() {
        return this.browser.waitForVisible(pageObject.logo(), 0);
    });

    it('should lead to yandex', function() {
        return this.browser
            .click(pageObject.logo())
            .getUrl()
            .then((currentUrl) => this.browser.shouldBeEqual(currentUrl, 'https://yandex.ru/'));
    });
});
