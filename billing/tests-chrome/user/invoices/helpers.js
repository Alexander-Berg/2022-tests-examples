module.exports.hideElements = ['td.b-payments__nowrap', 'td.b-payments__td:nth-child(7)'];

module.exports.setDate = async function (browser, selector) {
    await browser.click(selector + ' input:nth-child(3)');
    await browser.waitForVisible('.dates .date');
    await browser.click('.dates .date');
    await browser.ybWaitForInvisible('#datepick-div');
};
