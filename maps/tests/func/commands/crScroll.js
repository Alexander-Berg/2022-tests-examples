/*eslint prefer-arrow-callback: "off"*/

/**
 * Скролит к нужному элементу
 *
 * @name browser.crScroll
 * @param {String|Number} value or number of pixels from top
 *
 */
module.exports = function (value) {
    /**
     * Скролит к нужному элементу
     *
     * @param {Browser} browser
     * @param {String} selector
     *
     * @returns {Promise}
     */
    function scrollToElement(browser, selector) {
        return browser.execute(function (selector) {
            document.querySelector(selector).scrollIntoView();
        }, selector);
    }

    /**
     * Скролит страницу на указанное кол-во пикселей
     *
     * @param {Browser} browser
     * @param {Number} number
     *
     * @returns {Promise}
     */
    function scrollPixels(browser, number) {
        return browser
        // для chrome-phone
            .execute(function (number) {
                document.body.scrollTop = number;
            }, number)
            // для iphone
            .scroll(0, number);
    }
    return typeof value === 'string' ?
        scrollToElement(this, value) : scrollPixels(this, value);
};
