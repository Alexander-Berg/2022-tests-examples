const { basicHide, basicIgnore } = require('../../../helpers');

module.exports.assertViewOpts = {
    hideElements: basicHide,
    ignoreElements: basicIgnore
};

module.exports.wideAssertViewOpts = {
    hideElements: basicHide,
    ignoreElements: basicIgnore,
    captureElementFromTop: true,
    allowViewportOverflow: true,
    compositeImage: true,
    expandWidth: true
};

module.exports.waitTimeoutForExtensiveQuery = 120000;

async function setClient(browser, clientId) {
    await browser.click('.yb-partner-contracts-search__client .Textinput');
    await browser.ybReplaceValue('.yb-clients-search__client-id', clientId);
    await browser.ybFilterDoModalSearch();
    await browser.waitForVisible('.yb-clients-table__select-client');
    await browser.click('.yb-clients-table__select-client');
}

async function setPerson(browser, personId) {
    await browser.click('.yb-partner-contracts-search__person .Textinput');
    await browser.ybReplaceValue('.yb-persons-search__person-id', personId);
    await browser.ybFilterDoModalSearch();
    await browser.waitForVisible('.yb-persons-table__select-person');
    await browser.click('.yb-persons-table__select-person');
}

module.exports['РСЯ'] = {
    async setValues(browser) {
        await browser.ybSetSteps(`Заполняет страницу данными`);
        await browser.ybSetLcomSelectValue('.yb-partner-contracts-search__contract-class', 'РСЯ');
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__firm-id',
            'Yandex Europe AG'
        );
        await setClient(browser, '62103322');
        await setPerson(browser, '7869373');
        await browser.ybSetDatepickerValue(
            '.yb-partner-contracts-search__dt-from',
            '01.01.2020 г.'
        );
        await browser.ybSetDatepickerValue('.yb-partner-contracts-search__dt-to', '31.01.2020 г.');
        await browser.ybReplaceValue(
            '.yb-partner-contracts-search__contract-eid',
            'YAN-119717-01/20'
        );
        await browser.ybSetLcomSuggestValue('.yb-partner-contracts-search__manager', {
            searchValue: 'Группа РСЯ',
            exactMatch: true
        });
        await browser.ybSetLcomSelectValue('.yb-partner-contracts-search__contract-type', 'Оферта');
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__bill-interval',
            'Акт раз в месяц'
        );
    },

    valuesUrl:
        'partner-contracts.xml?client_id=62103322&contract_class=PARTNERS&contract_eid=YAN-119717-01%2F20&contract_eid_like=0&contract_type=9&dt_from=2020-01-01T00%3A00%3A00&dt_to=2020-01-31T00%3A00%3A00&date_type=1&doc_set=&firm=7&is_offer=&manager_code=20468&payment_type=1&person_id=7869373&platform_type=&pn=1&ps=10&service_id=&sf=start_dt&so=1&tag_id=&tag_name=',
    firstContractId: '1179093'
};

async function setTagAndAssertView(browser) {
    await browser.waitForVisible('.yb-partner-contracts-search__tag-name input');
    await browser.ybReplaceValue('.yb-partner-contracts-search__tag-name', 'mixmuz');
    await browser.waitForVisible('.yb-suggest__item');

    await browser.ybAssertView(
        'фильтр Дистрибуция, ввода тега в модалке',
        '.yb-search-filter',
        module.exports.assertViewOpts
    );

    await browser.ybReplaceValue('.yb-partner-contracts-search__tag-name', 'mixmuz 20 PPI');
    await browser.waitForVisible('.yb-suggest__item');

    await browser.click('.yb-suggest__item');
}

module.exports['Дистрибуция'] = {
    async setValues(browser) {
        await browser.ybSetSteps(`Заполняет страницу данными`);
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__contract-class',
            'Дистрибуция'
        );
        await browser.ybSetLcomSelectValue('.yb-partner-contracts-search__firm-id', 'ООО «Яндекс»');
        await setClient(browser, '61471390');
        await setPerson(browser, '9123873');
        await browser.ybSetDatepickerValue(
            '.yb-partner-contracts-search__dt-from',
            '01.01.2020 г.'
        );
        await browser.ybSetDatepickerValue('.yb-partner-contracts-search__dt-to', '31.01.2020 г.');
        await browser.ybReplaceValue(
            '.yb-partner-contracts-search__contract-eid',
            'ДС-29207-01/20'
        );
        await browser.ybSetLcomSuggestValue('.yb-partner-contracts-search__manager', {
            searchValue: 'Романова Татьяна Владимировна',
            exactMatch: true
        });
        await setTagAndAssertView(browser);
        await browser.ybReplaceValue('.yb-partner-contracts-search__tag-id', '4598');
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__platform-type',
            'Десктопный'
        );
    },

    valuesUrl:
        'partner-contracts.xml?client_id=61471390&contract_class=DISTRIBUTION&contract_eid=%D0%94%D0%A1-29207-01%2F20&contract_eid_like=0&contract_type=&dt_from=2020-01-01T00%3A00%3A00&dt_to=2020-01-31T00%3A00%3A00&date_type=1&doc_set=&firm=1&is_offer=&manager_code=38549&payment_type=&person_id=9123873&platform_type=2&pn=1&ps=10&service_id=&sf=start_dt&so=1&tag_id=4598&tag_name=mixmuz%2020%20PPI',
    firstContractId: '1228379'
};

module.exports['Справочник'] = {
    async setValues(browser) {
        await browser.ybSetSteps(`Заполняет страницу данными`);
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__contract-class',
            'Справочник'
        );
        await browser.ybReplaceValue('.yb-partner-contracts-search__contract-eid', '243-12\\10');
    },

    valuesUrl:
        'partner-contracts.xml?contract_class=GEOCONTEXT&contract_eid=243-12%5C10&contract_eid_like=0&contract_type=&date_type=1&doc_set=&firm=&is_offer=&payment_type=&platform_type=&pn=1&ps=10&service_id=&sf=start_dt&so=1&tag_id=&tag_name=',
    firstContractId: '61519'
};

module.exports['Афиша'] = {
    async setValues(browser) {
        await browser.ybSetSteps(`Заполняет страницу данными`);
        await browser.ybSetLcomSelectValue('.yb-partner-contracts-search__contract-class', 'Афиша');
        await browser.ybReplaceValue('.yb-partner-contracts-search__contract-eid', '121');
    },

    valuesUrl:
        'partner-contracts.xml?contract_class=AFISHA&contract_eid=121&contract_eid_like=0&contract_type=&date_type=1&doc_set=&firm=&is_offer=&payment_type=&platform_type=&pn=1&ps=10&service_id=&sf=start_dt&so=1&tag_id=&tag_name=',
    firstContractId: '69922'
};

module.exports['Приоритетная сделка'] = {
    async setValues(browser) {
        await browser.ybSetSteps(`Заполняет страницу данными`);
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__contract-class',
            'Приоритетная сделка'
        );
        await browser.ybReplaceValue(
            '.yb-partner-contracts-search__contract-eid',
            '3f13b6802c554086a69aa5c60ab2018f'
        );
    },

    valuesUrl:
        'partner-contracts.xml?contract_class=PREFERRED_DEAL&contract_eid=3f13b6802c554086a69aa5c60ab2018f&contract_eid_like=0&contract_type=&date_type=1&doc_set=&firm=&is_offer=&payment_type=&platform_type=&pn=1&ps=10&service_id=&sf=start_dt&so=1&tag_id=&tag_name=',
    firstContractId: '180008'
};

module.exports['Расходный'] = {
    async setValues(browser) {
        await browser.ybSetSteps(`Заполняет страницу данными`);
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__contract-class',
            'Расходный'
        );
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__firm-id',
            'ООО «Яндекс.Такси»'
        );
        await setClient(browser, '34616701');
        await setPerson(browser, '4499323');
        await browser.ybSetLcomSelectValue('.yb-partner-contracts-search__dt-type', 'Окончания');
        await browser.ybSetDatepickerValue(
            '.yb-partner-contracts-search__dt-from',
            '01.01.2020 г.'
        );
        await browser.ybSetDatepickerValue('.yb-partner-contracts-search__dt-to', '31.01.2020 г.');
        await browser.ybReplaceValue('.yb-partner-contracts-search__contract-eid', '14638');
        await browser.ybSetLcomCheckboxValue(
            '.yb-partner-contracts-search__contract-eid-like',
            true
        );
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__offer',
            'Двусторонний договор'
        );
        await browser.ybSetLcomSuggestValue('.yb-partner-contracts-search__manager', {
            searchValue: 'Кузовлева Алена Павловна',
            exactMatch: true
        });
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__service-id',
            'Яндекс.Корпоративное Такси'
        );
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__bill-interval',
            'Акт раз в месяц'
        );
    },

    valuesUrl:
        'partner-contracts.xml?client_id=34616701&contract_class=SPENDABLE&contract_eid=14638&contract_eid_like=1&contract_type=&dt_from=2020-01-01T00%3A00%3A00&dt_to=2020-01-31T00%3A00%3A00&date_type=2&doc_set=&firm=13&is_offer=0&manager_code=30474&payment_type=1&person_id=4499323&platform_type=&pn=1&ps=10&service_id=135&sf=start_dt&so=1&tag_id=&tag_name=',
    firstContractId: '293464'
};

module.exports['Эквайринг'] = {
    async setValues(browser) {
        await browser.ybSetSteps(`Заполняет страницу данными`);
        await browser.ybSetLcomSelectValue(
            '.yb-partner-contracts-search__contract-class',
            'Эквайринг'
        );
        await browser.ybReplaceValue('.yb-partner-contracts-search__contract-eid', '1826');
    },

    valuesUrl:
        'partner-contracts.xml?contract_class=ACQUIRING&contract_eid=1826&contract_eid_like=0&contract_type=&date_type=1&doc_set=&firm=&is_offer=&payment_type=&platform_type=&pn=1&ps=10&service_id=&sf=start_dt&so=1&tag_id=&tag_name=',
    firstContractId: '260364'
};

module.exports.clearFilter = async function (browser) {
    await browser.ybSetSteps('Сбрасывает фильтр');
    await browser.click('.yb-search-filter__button-clear');
};

module.exports.paginationUrl =
    'partner-contracts.xml?contract_class=SPENDABLE&contract_eid=&contract_eid_like=0&contract_type=&dt_from=2020-02-01T00%3A00%3A00&dt_to=2020-02-29T00%3A00%3A00&date_type=2&doc_set=&firm=&is_offer=&payment_type=&platform_type=&pn=1&ps=10&service_id=&sf=start_dt&so=1&tag_id=&tag_name=';
