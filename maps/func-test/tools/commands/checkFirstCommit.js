const pageObject = require('../../page-object.js');

/**
 * @name browser.checkFirstCommit
 * @param {String} obj
 */
module.exports = function(obj) {
    return this
        .debugLog('Checking ' + obj + ' history')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.geoObjCommitsViewFooter())
        .verifyScreenshot(pageObject.sidebarViewIsland(), obj + '-history')
        .pointerClick(pageObject.geoObjCommitsViewFooter())
        .waitForVisible(pageObject.commitDiffView())
        .verifyScreenshot(pageObject.geoObjCommitsViewFooter(), obj + '-commit1')
        .keys('Backspace')
        .waitForVisible(pageObject.geoObjViewerView.commentsLink());
};
