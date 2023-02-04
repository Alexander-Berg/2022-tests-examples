/**
 *
 * @name browser.crDeleteFirstMap
 * @param {Boolean} [isNoWait]
 */
module.exports = function (isNoWait) {
    return this
        .click(PO.mapSelection.itemFirstMenuBtn())
        .crShouldBeVisible(PO.mapSelectionMenu())
        .click(PO.mapSelectionMenu.menuDelete())
        .alertAccept()
        .then(() => {
            if (!isNoWait) {
                return this.crWaitForVisible(PO.mapSelection.itemFirst(), 'Не загрузился список карт после удаления');
            }
        });
};
