const { basicHide, basicIgnore } = require('../../../helpers');
const {
    setValues,
    setValuesSmoke,
    valuesUrl,
    paginationUrl,
    setClientAndInvoice,
    confirmPaymentSelector,
    confirmPayment,
    tableHide,
    waitTimeoutForExtensiveQuery
} = require('./search.helpers');

describe('admin', function () {
    describe('invoices', function () {
        it('корректность заполнения полей на основе заготовленного url и проверка вывода итогов, сброса', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'invoices.xml');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await setValues(browser);
            await browser.ybFilterDoSearch({ timeout: waitTimeoutForExtensiveQuery });
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await browser.ybAssertUrl(valuesUrl);
            await browser.ybClickOut();

            await browser.ybAssertView(
                'страница, заполнение фильтра и отображение таблицы',
                'body',
                {
                    hideElements: [...basicHide, '.yb-invoices-search__paysys-selector'],
                    ignoreElements: basicIgnore,
                    allowViewportOverflow: true,
                    expandWidth: true
                }
            );

            await browser.ybFilterClear();

            await browser.ybAssertView('фильтр, сброс', '.yb-invoices-search', {
                ignoreElements: basicIgnore
            });
        });

        it('переключение страниц, количества элементов', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', paginationUrl);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.ybTableChangeSort('Дата включения', '.yb-invoices-table');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.ybAssertView('список, переключение номера страницы', '.yb-content', {
                ignoreElements: basicIgnore,
                allowViewportOverflow: true,
                captureElementFromTop: true,
                expandWidth: true
            });

            await browser.ybTableChangePageSize(25);
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await browser.ybTableScrollToEnd();

            await browser.ybAssertView(
                'список, переключение размера страницы',
                '.yb-invoices-table',
                {
                    ignoreElements: basicIgnore,
                    allowViewportOverflow: true,
                    captureElementFromTop: true
                }
            );
        });

        it('поиск счетов [smoke]', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'invoices.xml');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await setValuesSmoke(browser);
            await browser.ybFilterDoSearch({ timeout: waitTimeoutForExtensiveQuery });
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await browser.ybAssertUrl(valuesUrl);
            await browser.ybClickOut();

            await browser.ybAssertView(
                'страница, заполнение фильтра и отображение таблицы [smoke]',
                '.yb-content',
                {
                    hideElements: [
                        ...basicHide,
                        '.yb-invoices-search__paysys-selector',
                        '.yb-search-list__list td:nth-child(10)'
                    ],
                    ignoreElements: basicIgnore,
                    allowViewportOverflow: true,
                    expandWidth: true
                }
            );

            await browser.ybFilterClear();

            await browser.ybAssertView('фильтр, сброс', '.yb-invoices-search', {
                ignoreElements: basicIgnore
            });
        });

        it('переход по ссылкам', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl(
                'admin',
                'invoices.xml?date_type=1&payment_status=0&post_pay_type=0&trouble_type=0&contract_eid=ВЭ262%2F0306&pn=1&ps=10&sf=invoice_dt&so=1'
            );
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.ybAssertLink('a=Б-1482523-1', 'invoice.xml?invoice_id=1210725');
            await browser.ybAssertLink('a=ВИ ИМХО (393872)', 'tclient.xml?tcl_id=393872');
            await browser.ybAssertLink('a=ВЭ262/0306', 'contract.xml?contract_id=4990');
        });

        it('кнопку оплата', async function () {
            const { browser } = this;

            const [clientId, , externalId] = await browser.ybRun('test_prepayment_unpaid_invoice');
            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'invoices.xml');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await setClientAndInvoice(browser, clientId, externalId);
            await browser.ybFilterDoSearch();
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.scroll(confirmPaymentSelector);
            await browser.ybAssertView(
                'включение заказа, отображение на неоплаченном',
                confirmPaymentSelector,
                {
                    ignoreElements: basicIgnore
                }
            );
            await confirmPayment(browser);
        });

        it('отсутствие кнопки оплата без права CreateBankPayments', async function () {
            const { browser } = this;

            const [clientId, , externalId] = await browser.ybRun('test_prepayment_unpaid_invoice');
            await browser.ybSignIn({ isAdmin: true, isReadonly: true });
            await browser.ybUrl('admin', 'invoices.xml');
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });
            await setClientAndInvoice(browser, clientId, externalId);
            await browser.ybFilterDoSearch();
            await browser.ybWaitForLoad({
                waitFilter: true,
                filterTimeout: waitTimeoutForExtensiveQuery
            });

            await browser.scroll(confirmPaymentSelector);
            await browser.ybAssertView(
                'таблица, отображение без права CreateBankPayments',
                '.yb-invoices-table',
                {
                    ignoreElements: basicIgnore,
                    hideElements: tableHide
                }
            );
        });
    });
});
