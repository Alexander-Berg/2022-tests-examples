const pageObject = require('../../page-object.js');

/**
 * @name browser.checkHistoryCommits
 * @param {String} obj
 */
module.exports = function(obj) {
    return this
        .debugLog('Checking ' + obj + ' history')
        .pointerClick(pageObject.geoObjViewerView.historyLink())
        .waitForVisible(pageObject.commitView())
        .verifyScreenshot(pageObject.sidebarViewIsland(), obj + '-history')
        .pointerClick(pageObject.commitViewCommit())
        .waitForVisible(pageObject.commitDiffView())
        .verifyScreenshot(pageObject.commitView(), obj + '-commit1')
        .keys('Backspace')
        .waitForVisible(pageObject.geoObjViewerView.commentsLink());
};
