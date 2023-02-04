/**
 * @name browser.waitForExist
 * */
module.exports = function (selector, timeout) {
    return this.waitUntil(async () => {
        const elements = await this.$$(selector);
        if (elements.length === 0) {
            return false;
        }
        const areExisting = await Promise.all(elements.map(e => e.isExisting()));
        return areExisting.every(Boolean);
    }, {timeout});
};
