const { WAIT_FOR_TILES_TIMEOUT } = require('../constants'),
    pageObject = require('../../page-object.js');

/**
 * @name browser.waitForYMapsTilesLoaded
 * */
module.exports = function() {
    return this.waitForExist('.' + pageObject.tilesLoaded(), WAIT_FOR_TILES_TIMEOUT);
};
