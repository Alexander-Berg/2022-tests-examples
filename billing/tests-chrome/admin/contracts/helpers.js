async function setClient(browser) {
    await browser.click('.yb-contracts-search__client .Textinput');
    await browser.ybReplaceValue('.yb-clients-search__client-id', '805242');
    await browser.ybFilterDoModalSearch();
    await browser.waitForVisible('.yb-clients-table__select-client');
    await browser.click('.yb-clients-table__select-client');
}

async function setPerson(browser) {
    await browser.click('.yb-contracts-search__person .Textinput');
    await browser.ybReplaceValue('.yb-persons-search__person-id', '1243320');
    await browser.ybFilterDoModalSearch();
    await browser.waitForVisible('.yb-persons-table__select-person');
    await browser.click('.yb-persons-table__select-person');
}

module.exports.setValues = async function (browser) {
    await browser.ybSetSteps(`Заполняет страницу данными`);
    await setClient(browser);
    await setPerson(browser);
    await browser.ybReplaceValue('.yb-contracts-search__contract-eid', '24576/13');
    await browser.ybSetLcomSelectValue('.yb-contracts-search__contract-type', 'Не агентский');
    await browser.ybSetLcomSelectValue(
        '.yb-contracts-search__service-id',
        'Директ: Рекламные кампании'
    );
    await browser.ybSetLcomSelectValue('.yb-contracts-search__payment-type', 'Постоплата');
    await browser.ybSetLcomSelectValue('.yb-contracts-search__dt-type', 'Начала');
    await browser.ybSetDatepickerValue('.yb-contracts-search__dt-from', '28.10.2013 г.');
    await browser.ybClickOut();
    await browser.ybSetDatepickerValue('.yb-contracts-search__dt-to', '30.10.2013 г.');
    await browser.ybClickOut();
};

module.exports.valuesUrl =
    'contracts.xml?date_from=2013-10-28T00%3A00%3A00&date_to=2013-10-30T00%3A00%3A00&client_id=805242&person_id=1243320&date_type=1&commission=0&service_id=7&payment_type=3&contract_eid=24576%2F13&ps=10&pn=1&sf=dt&so=1';

module.exports.paginationUrl =
    'contracts.xml?date_from=2015-04-02T00%3A00%3A00&date_to=2015-04-10T00%3A00%3A00&date_type=2&commission=&service_id=&payment_type=&ps=10&pn=1&sf=dt&so=1';

module.exports.waitTimeoutForExtensiveQuery = 90000;

module.exports.changeSort = async function (browser) {
    await browser.ybSetSteps(`Переключает на сортировку по номеру договора`);
    const elem = await browser.ybWaitForExist(['.yb-contracts-table', 'th=№ договора']);
    await elem.click();

    await browser.ybWaitForLoad({
        waitFilter: true,
        filterTimeout: module.exports.waitTimeoutForExtensiveQuery
    });
};
