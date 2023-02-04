const assert = require('chai').assert;

module.exports.openPerson = async function (browser) {
    await browser.ybSetSteps(`Открывает селектор плательщика`);
    await browser.click('.yb-invoices-search__person .Textinput');
};

module.exports.setPerson = async function (browser) {
    await browser.ybSetSteps(`Заполняет фильтр плательщика`);
    await browser.ybReplaceValue('.yb-persons-search__name', 'КУМИТ');
    await browser.ybLcomSelect('.yb-persons-search__person-type', 'Юр. лицо');
    await browser.ybReplaceValue('.yb-persons-search__person-id', '5687027');
    await browser.ybReplaceValue('.yb-persons-search__inn', '7707813050');
    await browser.ybReplaceValue('.yb-persons-search__email', 'coomeetyour@yandex.ru');
    await browser.ybLcomSelect('.yb-persons-search__is-partner', 'Только не партнеры');
    await browser.ybSetLcomCheckboxValue('.yb-persons-search__vip-only', true);
};

module.exports.clearFilter = async function (browser) {
    await browser.ybSetSteps(`Сбрасывает фильтр плательщика`);
    await browser.click('.Modal_visible .yb-search-filter__button-clear');
};

module.exports.selectByName = async function (browser) {
    await browser.ybSetSteps(`Выбирает плательщика по названию`);
    await browser.click('=КУМИТ (5687027)');
};

module.exports.assertPersonSelectorName = async function (browser, name) {
    const realName = await browser.getValue(`.yb-invoices-search__person input`);
    await browser.ybSetSteps(`Проверяет что текст селектора равен ` + name);
    assert.equal(realName, name, 'Текст селектора плательщтика не совпадает');
};

module.exports.clearSelectedPerson = async function (browser) {
    await browser.ybSetSteps(`Сбрасывает выбранного плательщика`);
    await browser.click(`.yb-invoices-search__person button`);
};

module.exports.closeModal = async function (browser) {
    await browser.ybSetSteps(`Закрывает модалку`);
    await browser.click('.Modal_visible #modal-close-btn-null');
};

module.exports.selectByChooseLink = async function (browser) {
    await browser.ybSetSteps(`Выбирает плательщика по кнопке выбрать`);
    await browser.click('=Выбрать');
};

module.exports.setPaginatablePerson = async function (browser) {
    await browser.ybSetSteps(`Заполняет фильтр плательщика`);
    await browser.ybReplaceValue('.yb-persons-search__name', 'ООО "Гермионовый плательщик"');
    await browser.ybReplaceValue('.yb-persons-search__inn', '2473746582');
    await browser.ybReplaceValue('.yb-persons-search__email', 'hermione_test_pers@ya.ru');
};

module.exports.waitTimeoutForExtensiveQuery = 120000;

module.exports.personSelectorContentSelector =
    '.src-common-presentational-components-ModalWindow-___style-module__content';
module.exports.personsTableSelector = '.yb-persons-table';

module.exports.setPersonById = async function (browser) {
    await browser.ybSetSteps(`Заполняет фильтр плательщика`);
    await browser.ybReplaceValue('.yb-persons-search__person-id', '5687027');
};

module.exports.hideElements = ['.yb-persons-table__person', '.yb-persons-table__client'];
