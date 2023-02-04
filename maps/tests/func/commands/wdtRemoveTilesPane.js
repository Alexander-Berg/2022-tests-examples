/**
 *
 * @name browser.wdtRemoveTilesPane
 *
 */

module.exports = function () {
    const command = this.executionContext.browserId === 'edge' ? 'waitForExist' : 'waitForVisible';

    return this[command](PO.ymaps.groundPane())
        .execute(function cb(cl) {
            const elem = document.querySelector(cl);
            elem.parentNode.removeChild(elem);
        }, PO.ymaps.groundPane());
};
