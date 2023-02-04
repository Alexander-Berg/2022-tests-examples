const { basicIgnore } = require('../../../helpers');

module.exports.setValues = async function (browser) {
    await browser.ybSetSteps('Заполняет страницу данными');
    await browser.ybSetDatepickerValue('.yb-requests-search__dt-from', '31.10.2016 г.');
    await browser.ybSetDatepickerValue('.yb-requests-search__dt-to', '10.12.2019 г.');
    await browser.ybReplaceValue('.yb-requests-search__request-id', '334561615');
    await setClient(browser, '32889174');
};

async function setClient(browser, clientId) {
    await browser.click('.yb-requests-search__client .Textinput');
    await browser.ybReplaceValue('.yb-clients-search__client-id', clientId);
    await browser.ybFilterDoModalSearch();
    await browser.waitForVisible('.yb-clients-table__select-client');
    await browser.click('.yb-clients-table__select-client');
}

module.exports.valuesUrl =
    'requests.xml?dt_from=2016-10-31T00%3A00%3A00&dt_to=2019-12-10T00%3A00%3A00&client_id=32889174&request_id=334561615&pn=1&ps=10&sf=DT&so=1';

module.exports.assertViewOpts = {
    ignoreElements: basicIgnore
};

module.exports.clearFilter = async function (browser) {
    await browser.ybSetSteps('Сбрасывает фильтр');
    await browser.click('.yb-search-filter__button-clear');
};

module.exports.paginationUrl =
    'requests.xml?dt_from=2015-07-31T00%3A00%3A00&dt_to=2017-12-11T00%3A00%3A00&client_id=5708488&pn=1&ps=10&sf=DT&so=1';
