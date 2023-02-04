/**
 * Команда для нажатия на клавишу с зажатым модификатором или без
 *
 * @name browser.crPressKey
 * @param {Object[]|String} key
 */
module.exports = function (key) {
    let modifier;

    if (Array.isArray(key) && ['Control', 'Command', 'Shift', 'Alt'].indexOf(key[0]) !== -1) {
        modifier = key[0];
    }

    return this.keys(key).then(() => {
        if (!modifier) {
            return true;
        }
        return this.keys(modifier);
    });
};
