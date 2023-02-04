module.exports.changePersonTypeToUrAutoru = async function (browser) {
    const url = await browser.url();
    await browser.ybAbsoluteUrl(url.replace('type=ur', `type=ur_autoru`));
};
