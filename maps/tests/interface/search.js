const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js');

require('../common.js')(beforeEach, afterEach);

describe('search', function() {
    beforeEach(function() {
        return this.browser.initNmaps('common');
    });

    it('should be visible', function() {
        return this.browser.waitForVisible(pageObject.search(), 0);
    });

    it('should show suggest and clear button', function() {
        return this.browser
            .pointerClick(pageObject.search.input())
            .setValue(pageObject.search.input(), 'Москва')
            .waitForVisible(pageObject.search.clear())
            .waitForVisible(pageObject.suggestPopup.items())
            .waitForVisible(pageObject.suggestTitle() + '=' + i18n('search', 'map-search'));
    });

    it('should suggest coordinates', function() {
        return this.browser
            .pointerClick(pageObject.search.input())
            .setValue(pageObject.search.input(), '59.5413, 29.7816')
            .waitForVisible(pageObject.search.clear())
            .waitForVisible(pageObject.suggestPopup.items())
            .waitForVisible(pageObject.suggestTitle() + '=' + i18n('search', 'by-coords'))
            .element(pageObject.search.input()).keys(['ArrowDown', 'Enter'])
            .waitForMapAt([29.7816, 59.5413]);
    });
});
