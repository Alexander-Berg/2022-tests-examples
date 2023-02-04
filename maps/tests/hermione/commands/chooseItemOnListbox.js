/**
 * Выбираем элемент из листбокса
 *
 * @name browser.chooseItemOnListbox
 * @param {String} selector - название item
 */
module.exports = function (selector) {
    return this
        .waitForVisible(PO.map.controls.listbox())
        .pointerClick(PO.map.controls.listbox())
        .waitAndClick(PO.mapControlsListboxItem() + '=' + selector)
        .waitAndClick(PO.map.controls.listbox())
        .waitForInvisible(PO.mapControlsListboxItem())
};