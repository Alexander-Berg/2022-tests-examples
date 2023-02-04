/**
 * @name browser.click
 * */
 module.exports = async function (selector) {
    const element = await this.$(selector);
    await element.click();
};
