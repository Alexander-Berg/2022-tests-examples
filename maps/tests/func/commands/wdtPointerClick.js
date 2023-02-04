/**
 *
 * @name browser.wdtPointerClick
 *
 */

module.exports = function (x, y) {
    if (this.executionContext.browserId === 'firefox') {
        return this.actions([{
            type: 'pointer',
            id: 'finger1',
            parameters: {pointerType: 'mouse'},
            actions: [
                {type: 'pointerMove', duration: 0, x: x, y: y},
                {type: 'pointerDown', button: 0},
                {type: 'pointerUp', button: 0}
            ]
        }]);
    }

    return this.leftClick(PO.ymaps.eventPane(), x, y);
};
