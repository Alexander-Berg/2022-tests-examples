const pageObject = require('../../page-object.js'),
    i18n = require('../../tools/i18n.js'),
    { ANIMATION_TIMEOUT } = require('../../tools/constants');

/**
 * @name browser.createGeoObjectBySuggest
 * @param {string} objectCategory
 */
module.exports = function(objectCategory) {
    const category = i18n('categories', objectCategory);

    return this.debugLog('Creating geoobject "' + objectCategory + '" starts')
        .pointerClick(pageObject.appBarView.create())
        .waitForVisible(pageObject.categoryView())
        .setValue(pageObject.suggestCategory.input(), category)
        .waitForExist(pageObject.suggestItems() + ' ' + pageObject.suggestItem())
        .pause(ANIMATION_TIMEOUT)
        .pointerClick(pageObject.suggestItem() + '=' + category)
        .waitForVisible(pageObject.geoObjEditorView())
        .debugLog('Geoobject is ready to be drawn');
};
