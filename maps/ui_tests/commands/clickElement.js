/**
 * Click panorama's html elements with certain text.
 * 
 * docs: https://webdriver.io/docs/selectors/#element-with-certain-text
 *
 * @name browser.clickElement
 */
module.exports = async function(selector) {
    const element = await this.$(selector);
    await element.click({ button: 'left'});
};