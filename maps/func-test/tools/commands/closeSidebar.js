const pageObject = require('../../page-object.js');

/**
 * @name browser.closeSidebar
 */
module.exports = function() {
    return this.debugLog('Closing sidebar')
        .waitForVisible(pageObject.geoObjViewerView.close(), 450)
        .pointerClick(pageObject.geoObjViewerView.close())
        .waitForInvisible(pageObject.geoObjViewerView());
};
