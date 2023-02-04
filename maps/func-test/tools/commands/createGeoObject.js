const pageObject = require('../../page-object.js'),
    geoObjectMenu = require('../createGeoObjectMenuConfig.js');

/**
 * @name browser.createGeoObject
 * @param {string} templateId
 */
module.exports = function(templateId) {
    const templateIdMap = geoObjectMenu[templateId];
    let templateIdGroupNumber,
        templateIdListGroupNumber,
        templateIdNumber;

    if(typeof templateIdMap === 'undefined') {
        throw new Error('Unknown templateId ' + templateId);
    }

    if(typeof templateIdMap === 'number') {
        templateIdGroupNumber = templateIdMap;
    }

    else {
        templateIdGroupNumber = templateIdMap[0];
        templateIdListGroupNumber = templateIdMap[1];
        templateIdNumber = templateIdMap[1];
        templateIdMap.length === 2? templateIdListGroupNumber = 1 : templateIdNumber = templateIdMap[2];
    }

    return this.debugLog('Creating geoobject "' + templateId + '" starts')
        .pointerClick(pageObject.appBarView.create())
        .waitForVisible(pageObject.categorySelectorGroupsView.group() + ':nth-child(' + templateIdGroupNumber + ')')
        .pointerClick(pageObject.categorySelectorGroupsView.group() + ':nth-child(' + templateIdGroupNumber + ')')
        .isVisible(pageObject.geoObjEditorView())
        .then(isReady => isReady || !templateIdListGroupNumber || this.isVisible(pageObject.listItemGroup())
            .then(() => {
                const selector = pageObject.listItemGroup() + ':nth-child(' + templateIdListGroupNumber + ') ' +
                    pageObject.listItemView() + ':nth-child(' + templateIdNumber + ')';

                return this
                    .waitForVisible(selector)
                    .pointerClick(selector)
                    .waitForVisible(pageObject.geoObjEditorView());
            })
        )
        .debugLog('Geoobject is ready to be drawn');
};
