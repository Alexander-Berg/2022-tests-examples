module.exports.changePersonTypeToPhAutoru = async function (browser) {
    const url = await browser.url();
    await browser.ybAbsoluteUrl(url.replace('type=ph', `type=ph_autoru`));
};
