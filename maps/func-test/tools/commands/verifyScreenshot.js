const pageObject = require('../../page-object.js');

/**
 * @name browser.verifyScreenshot
 * @param {String} selector
 * @param {String} name
 * @param {Object} [options]
 */
module.exports = function(selector, name, options = {}) {
    const covered = pageObject.appView.map();
    return this
        .debugLog(`Verifying screenshot "${name}"...`)
        .execute(function(covered) {
            const coveredElement = document.querySelectorAll(covered)[0];
            const element = window.document.createElement('div');
            const style = `
                position: absolute;
                top: 0;
                height: 100%;
                width: 100%;
                background-color: black;
            `;
            element.setAttribute('id', 'map-cover');
            element.setAttribute('style', style);
            coveredElement.appendChild(element);
        }, covered)
        .catch((err) => this.reportError(err))
        .then(() => options.notMoveMouse? true : this.moveMouseAside())
        .waitForExist('div#map-cover')
        .assertView(name + '.chrome', selector, options)
        .debugLog(`Screenshot "${name}" is verified`)
        .execute(function(covered) {
            const coveredElement = document.querySelectorAll(covered)[0];
            const element = window.document.getElementById('map-cover');
            coveredElement.removeChild(element);
        }, covered)
        .catch((err) => this.reportError(err));
};
