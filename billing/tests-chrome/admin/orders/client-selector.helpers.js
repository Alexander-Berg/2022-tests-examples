const assert = require('chai').assert;

module.exports.openClient = async function (browser) {
    await browser.ybSetSteps(`Открывает селектор клиента`);
    await browser.click('.yb-orders-search__client .Textinput');
};

module.exports.setClient = async function (browser) {
    await browser.ybSetSteps(`Заполняет фильтр клиента`);
    await browser.ybReplaceValue('.yb-clients-search__name', 'Netpeak');
    await browser.ybReplaceValue('.yb-clients-search__login', 'netpeakru');
    await browser.ybReplaceValue('.yb-clients-search__client-id', '5028445');
    await browser.ybLcomSelect('.yb-clients-search__agency-select-policy', 'Агентства');
    await browser.ybReplaceValue('.yb-clients-search__url', 'http://netpeak.ua/');
    await browser.ybReplaceValue('.yb-clients-search__email', 'v.krasko@netpeak.net');
    await browser.ybReplaceValue('.yb-clients-search__phone', '+38 063 80 40 690');
};

module.exports.clearFilter = async function (browser) {
    await browser.ybSetSteps(`Сбрасывает фильтр клиента`);
    await browser.click('.Modal_visible .yb-search-filter__button-clear');
};

module.exports.selectByName = async function (browser) {
    await browser.ybSetSteps(`Выбирает клиента по названию`);
    await browser.click('=Netpeak (5028445)');
};

module.exports.assertClientSelectorName = async function (browser, name) {
    const realName = await browser.getValue(`.yb-orders-search__client input`);
    await browser.ybSetSteps(`Проверяет что текст селектора равен ` + name);
    assert.equal(realName, name, 'Текст селектора клиента не совпадает');
};

module.exports.clearSelectedClient = async function (browser) {
    await browser.ybSetSteps(`Сбрасывает выбранного клиента`);
    await browser.click(`.yb-orders-search__client button`);
};

module.exports.closeModal = async function (browser) {
    await browser.ybSetSteps(`Закрывает модалку`);
    await browser.click('.Modal_visible #modal-close-btn-null');
};

module.exports.selectByChooseLink = async function (browser) {
    await browser.ybSetSteps(`Выбирает клиента по кнопке выбрать`);
    await browser.click('=Выбрать');
};

module.exports.setPaginatableClient = async function (browser) {
    await browser.ybSetSteps(`Заполняет фильтр клиента`);
    await browser.ybReplaceValue('.yb-clients-search__login', 'yb-atst-herm-client');
};

module.exports.setClientById = async function (browser) {
    await browser.ybSetSteps(`Заполняет фильтр клиента`);
    await browser.ybReplaceValue('.yb-clients-search__client-id', '5028445');
};
