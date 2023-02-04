/**
 * @name browser.waitForVisible
 * */
module.exports = function (selector, timeout) {
    return this.waitUntil(async () => {
        const elements = await this.$$(selector);
        if (elements.length === 0) {
            return false;
        }
        const areDisplayed = await Promise.all(elements.map(e => e.isDisplayed()));
        return areDisplayed.every(Boolean);
    }, {timeout});
};
