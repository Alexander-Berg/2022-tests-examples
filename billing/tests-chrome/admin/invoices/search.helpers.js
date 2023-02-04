async function setClient(browser) {
    await browser.click('.yb-invoices-search__client .Textinput');
    await browser.ybReplaceValue('.yb-clients-search__client-id', '8637564');
    await browser.ybFilterDoModalSearch();
    await browser.waitForVisible('.yb-clients-table__select-client');
    await browser.click('.yb-clients-table__select-client');
}

async function setPerson(browser) {
    await browser.click('.yb-invoices-search__person .Textinput');
    await browser.ybReplaceValue('.yb-persons-search__person-id', '3034649');
    await browser.ybFilterDoModalSearch();
    await browser.waitForVisible('.yb-persons-table__select-person');
    await browser.click('.yb-persons-table__select-person');
}

async function setPaysys(browser) {
    await browser.click('.Button2_type_link=Способы оплаты');
    await browser.waitForVisible('.Checkbox-Label=Банк для юр.лиц, RUB, резидент, Россия');
    await browser.click('.Checkbox-Label=Банк для юр.лиц, RUB, резидент, Россия');
    await browser.click('.Modal_visible #modal-close-btn-null');
}

module.exports.setValues = async function (browser) {
    await browser.ybSetSteps(`Заполняет страницу данными`);
    await browser.ybSetDatepickerValue('.yb-invoices-search__date-from', '24.06.2019');
    await browser.ybClickOut();
    await browser.ybSetDatepickerValue('.yb-invoices-search__date-to', '26.06.2019');
    await browser.ybClickOut();
    await browser.ybReplaceValue('.yb-invoices-search__invoice-eid', 'Б-1782034219-1');
    await setClient(browser);
    await browser.ybLcomSelect('.yb-invoices-search__payment-status', 'Включенные');
    await browser.ybLcomSelect('.yb-invoices-search__firm', 'ООО «Яндекс»');
    await browser.ybLcomSelect('.yb-invoices-search__post-pay-type', 'Фиктивные');
    await browser.ybSetLcomCheckboxValue('.yb-invoices-search__show-inn', true);
    await setPerson(browser);
    await browser.ybLcomSelect('.yb-invoices-search__service', 'Директ: Рекламные кампании');
    await browser.ybReplaceValue('.yb-invoices-search__service-order-id', '7-17135232');
    await browser.ybReplaceValue('.yb-invoices-search__contract-eid', '96323/18');
    await browser.ybSetLcomCheckboxValue('.yb-search-filter__show-totals', true);

    await setPaysys(browser);
};

module.exports.setValuesSmoke = async function (browser) {
    await browser.ybSetSteps(`Заполняет страницу данными`);
    await browser.ybSetDatepickerValue('.yb-invoices-search__date-from', '24.06.2019');
    await browser.ybClickOut();
    await browser.ybSetDatepickerValue('.yb-invoices-search__date-to', '26.06.2019');
    await browser.ybClickOut();
    await browser.ybReplaceValue('.yb-invoices-search__invoice-eid', 'Б-1782034219-1');
    await setClient(browser);
    await browser.ybLcomSelect('.yb-invoices-search__payment-status', 'Включенные');
    await browser.ybLcomSelect('.yb-invoices-search__firm', 'ООО «Яндекс»');
    await browser.ybLcomSelect('.yb-invoices-search__post-pay-type', 'Фиктивные');
    await browser.ybSetLcomCheckboxValue('.yb-invoices-search__show-inn', true);
    await setPerson(browser);
    await browser.ybLcomSelect('.yb-invoices-search__service', 'Директ: Рекламные кампании');
    await browser.ybReplaceValue('.yb-invoices-search__service-order-id', '7-17135232');
    await browser.ybReplaceValue('.yb-invoices-search__contract-eid', '96323/18');
    await browser.ybSetLcomCheckboxValue('.yb-search-filter__show-totals', true);
};

module.exports.valuesUrl =
    'invoices.xml?date_type=1&dt_from=2019-06-24T00%3A00%3A00&dt_to=2019-06-26T00%3A00%3A00&invoice_eid=%D0%91-1782034219-1&payment_status=2&firm_id=1&post_pay_type=3&trouble_type=0&client_id=8637564&person_id=3034649&show_inn=1&service_id=7&service_order_id=7-17135232&contract_eid=96323%2F18&ct=1&pn=1&ps=10&sf=invoice_dt&so=1';
module.exports.paginationUrl =
    'invoices.xml?date_type=1&dt_from=2017-07-20T00%3A00%3A00&dt_to=2017-07-20T00%3A00%3A00&payment_status=0&post_pay_type=0&trouble_type=0&client_id=1020474&pn=1&ps=10&sf=invoice_dt&so=1';

module.exports.setClientAndInvoice = async function (browser, clientId, externalId) {
    await browser.ybSetSteps(`Заполняет клиента и счет`);
    await browser.click('.yb-invoices-search__client .Textinput');
    await browser.ybReplaceValue('.yb-clients-search__client-id', String(clientId));
    await browser.ybFilterDoModalSearch();
    await browser.waitForVisible('.yb-clients-table__select-client');
    await browser.click('.yb-clients-table__select-client');

    await browser.ybReplaceValue('.yb-invoices-search__invoice-eid', String(externalId));
};

module.exports.confirmPaymentSelector = '.yb-invoices-table__confirm-payment';

module.exports.confirmPayment = async function (browser) {
    await browser.ybSetSteps(`Подтверждает оплату и проверяет, что кнопка исчезла`);
    await browser.click(module.exports.confirmPaymentSelector);
    await browser.ybMessageAccept();
    await browser.ybWaitForInvisible('span=Оплата');
};

module.exports.tableHide = [
    '.yb-invoices-table__invoice-id',
    '.yb-invoices-table__invoice-date',
    '.yb-invoices-table__person',
    '.yb-invoices-table__client'
];

module.exports.waitTimeoutForExtensiveQuery = 60000;
