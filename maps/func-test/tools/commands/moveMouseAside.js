/**
 * @name browser.moveMouseAside
 */
const { ANIMATION_TIMEOUT } = require('../constants');

module.exports = function() {
    return this.moveToObject('body', 640, 640)
        .pause(ANIMATION_TIMEOUT);
};
