/**
 * @name browser.waitForTilesLoaded
 * */
module.exports = function () {
    return this.waitForExist('.' + PO.tilesLoaded(), 15000);
};
