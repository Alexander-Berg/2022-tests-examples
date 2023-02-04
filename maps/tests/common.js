const testCounter = require('../tools/test-counter');

module.exports = function(beforeEach, afterEach) {
    beforeEach(function() {
        return this.browser
            .setMeta('testStarted', +new Date())
            .setMeta('testNumber', testCounter());
    });

    afterEach(function() {
        return this.browser
            .getUrl().then((url) => this.browser.setMeta('nmapsUrl', url))
            .verifyNoErrors()
            .ensureLogoutFast();
    });
};
