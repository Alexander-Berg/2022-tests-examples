/**
 * @name browser.pointerClick
 *
 * @param {String|Number} selector
 * @param {Number} [y]
 * @returns {*}
 */
module.exports = function(selector, y) {
    let x;
    if(y) {
        x = selector;
        selector = 'body';
    }

    return this
        .isVisible(selector)
        .then(visible => visible || this.reportError(`Element ("${selector}") not visible, click is impossible`))
        .leftClick(selector, x, y);
};
