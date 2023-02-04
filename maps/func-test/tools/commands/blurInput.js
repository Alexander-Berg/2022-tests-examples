const _ = require('lodash');

/**
 * @name browser.blurInput
 * @param {String} selector
 */
module.exports = function(selector) {
    return this
        .isVisible(selector)
        .then((isVisible) => {
            if(!isVisible) {
                throw new Error(`Input is not visible: ${selector}`);
            }
            if(_.isArray(isVisible)) {
                throw new Error(`More than one input: ${selector}`);
            }
        })
        .execute((selector) => {
            return document.querySelector(selector).blur();
        }, selector);
};
