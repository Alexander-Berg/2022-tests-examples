module.exports.setValuesFull = async function (browser) {
    await browser.ybSetSteps(`Заполняет страницу данными`);
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__object-type', 'Договор');
    await browser.ybSetDatepickerValue('.yb-contracts-for-pdfsend-search__dt-from', '01.02.2019');
    await browser.ybSetDatepickerValue('.yb-contracts-for-pdfsend-search__dt-to', '01.02.2019');
    await browser.ybSetLcomSuggestValue('.yb-contracts-for-pdfsend-search__bo-manager', {
        searchValue: 'Воробьева Юлия Юрьевна',
        exactMatch: true
    });
    await browser.ybSetLcomSuggestValue('.yb-contracts-for-pdfsend-search__sales-manager', {
        searchValue: 'Алешина Татьяна Владимировна',
        exactMatch: true
    });
    await browser.ybLcomSelect(
        '.yb-contracts-for-pdfsend-search__service-id',
        'Яндекс.Корпоративное Такси (Клиенты)'
    );
    await browser.ybReplaceValue('.yb-contracts-for-pdfsend-search__contract-eid', '177081/19');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__contract-type', 'Коммерческий');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__firm-id', 'ООО «Яндекс.Такси»');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__payment-type', 'Предоплата');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__is-faxed', 'Да');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__is-email-enqueued', 'Да');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__is-signed', 'Нет');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__is-sent-original', 'Нет');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__is-atypical-conditions', 'Нет');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__is-booked', 'Да');
};

module.exports.setValuesPagination = async function (browser) {
    await browser.ybSetSteps(`Заполняет страницу данными`);
    await browser.ybSetDatepickerValue('.yb-contracts-for-pdfsend-search__dt-from', '21.02.2019');
    await browser.ybSetDatepickerValue('.yb-contracts-for-pdfsend-search__dt-to', '28.02.2019');
    await browser.ybLcomSelect('.yb-contracts-for-pdfsend-search__firm-id', 'ООО «Яндекс»');
};

module.exports.contractId = 597988;

module.exports.waitTimeoutForExtensiveQuery = 60000;

module.exports.tableHide = ['.yb-search-list__list tbody td:nth-child(8)'];
