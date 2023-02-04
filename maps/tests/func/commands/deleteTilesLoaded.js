/**
 * @name browser.deleteTilesLoaded
 * */
module.exports = function () {
    return this.execute(
        (tilesLoadedClass) => document.body.classList.remove(tilesLoadedClass),
        PO.tilesLoaded()
    );
};
