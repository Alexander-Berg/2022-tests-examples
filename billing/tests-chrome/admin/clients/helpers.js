const { basicHide, basicIgnore } = require('../../../helpers');

module.exports.setValues = async function (browser) {
    await browser.ybSetSteps('Заполняет страницу данными');
    await browser.ybReplaceValue('.yb-clients-search__name', 'Netpeak');
    await browser.ybReplaceValue('.yb-clients-search__login', 'netpeakru');
    await browser.ybReplaceValue('.yb-clients-search__client-id', '5028445');
    await browser.ybSetLcomSelectValue('.yb-clients-search__agency-select-policy', 'Агентства');
    await browser.ybReplaceValue('.yb-clients-search__url', 'http://netpeak.ua/');
    await browser.ybReplaceValue('.yb-clients-search__email', 'v.krasko@netpeak.net');
    await browser.ybReplaceValue('.yb-clients-search__phone', '+38 063 80 40 690');
    await browser.ybSetLcomSelectValue('.yb-clients-search__with-invoices', 'Только со счетами');
    await browser.ybSetLcomSuggestValue('.yb-clients-search__manager', {
        searchValue: 'Свинцицкий Андрей',
        exactMatch: true
    });
};

module.exports.setAllValues = async function (browser) {
    await module.exports.setValues(browser);
    await browser.ybReplaceValue('.yb-clients-search__single-account-number', '123456');
    await browser.ybReplaceValue('.yb-clients-search__fax', '+1234567890');
    await browser.ybSetLcomSelectValue('.yb-clients-search__intercompany', 'Yandex.Taxi AM');
    await browser.ybSetLcomCheckboxValue('.yb-clients-search__is-accurate', true);
    await browser.ybSetLcomCheckboxValue('.yb-clients-search__hide-managers', true);
    await browser.ybSetLcomCheckboxValue('.yb-clients-search__manual-suspect', true);
    await browser.ybSetLcomCheckboxValue('.yb-clients-search__reliable-client', true);
};

module.exports.assertViewOpts = {
    ignoreElements: basicIgnore,
    hideElements: [...basicHide, '.yb-clients-table__managers']
};

module.exports.valuesUrl =
    'clients.xml?agency_select_policy=3&client_id=5028445&email=v.krasko%40netpeak.net&fax=%2B1234567890&hide_managers=1&intercompany=AM31&is_accurate=1&login=netpeakru&manual_suspect=1&name=Netpeak&phone=%2B38%20063%2080%2040%20690&reliable_cc_payer=1&single_account_number=123456&with_invoices=1&url=http%3A%2F%2Fnetpeak.ua%2F&pn=1&ps=10&manager_code=20869';

module.exports.clearFilter = async function (browser) {
    await browser.ybSetSteps('Сбрасывает фильтр');
    await browser.click('.yb-search-filter__button-clear');
};

module.exports.paginationUrl =
    'clients.xml?agency_select_policy=1&name=очень старый клиент&pn=1&ps=10&manager_code=1154';
