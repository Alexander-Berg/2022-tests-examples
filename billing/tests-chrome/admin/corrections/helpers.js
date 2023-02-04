module.exports.setClient = async function (browser, clientId) {
    await browser.ybSetSteps(`Заполняет клиента`);
    await browser.click('.yb-form-section div:nth-child(1) .Textinput');
    await browser.ybReplaceValue('.yb-clients-search__client-id', String(clientId));
    await browser.ybFilterDoModalSearch();
    await browser.waitForVisible('.yb-clients-table__select-client');
    await browser.click('.yb-clients-table__select-client');
};
