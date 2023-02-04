const { assertViewOpts } = require('./helpers');
const { basicIgnore } = require('../../../helpers');

describe('admin', function () {
    describe('invoice', function () {
        describe('информация о счете и платежи', function () {
            it('перенос срока оплаты, просроченный overdraft', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, id] = await browser.ybRun('test_overdraft_overdue_invoice');
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
                await browser.ybWaitForLoad();

                let hideElements = ['.yb-invoice-info .yb-table__client'];
                let options = {
                    ignoreElements: basicIgnore,
                    hideElements,
                    expandWidth: true
                };
                // проверяем наличие блока с переносом срока оплаты, указание срока оплаты
                await browser.ybAssertView(
                    'информация о счете, просроченный overdraft',
                    '.yb-invoice-info',
                    options
                );

                await browser.click('.yb-invoice-info .react-datepicker__input-container input');
                await browser.click(
                    '.react-datepicker__day--001.react-datepicker__day--outside-month'
                );
                options = {
                    ignoreElements: ['.yb-invoice-info .react-datepicker__input-container input'],
                    hideElements,
                    expandWidth: true
                };
                // выбираем новый срок оплаты, проверяем активность кнопки
                await browser.ybAssertView(
                    'информация о счете, просроченный overdraft, новый срок оплаты',
                    '.yb-invoice-info',
                    options
                );

                await browser.click('.yb-invoice-info .Button2_theme_action');
                await browser.waitForVisible(
                    '.src-admin-pages-invoice-components-InvoiceInfo-' +
                        '___invoice-info-module__add-extension-form__success'
                );
                options = {
                    hideElements: [
                        ...hideElements,
                        '.yb-invoice-info > section:nth-child(2) > div > div > table'
                    ],
                    expandWidth: true
                };
                // проверяем вид блока после переноса срока оплаты
                await browser.ybAssertView(
                    'информация о счете, просроченный overdraft, после переноса срока',
                    '.yb-invoice-info',
                    options
                );
            });

            it('просмотр информации о плательщике в блоке Платежи', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=79278002');
                await browser.waitForVisible('.yb-table_payments1c');

                let options = {
                    expandWidth: true
                };
                await browser.ybAssertView(
                    'таблица Платежи',
                    '.yb-invoice-info > section:nth-child(2)',
                    options
                );

                // проверяем поп-ап в ИНН
                await browser.click('.yb-table_payments1c tr:nth-child(1) td:nth-child(7) a');
                await browser.ybAssertView(
                    'таблица Платежи, поп-ап ИНН',
                    '.yb-invoice-info > section:nth-child(2)',
                    options
                );

                // закрываем поп-ап в ИНН, проверяем поп-ап в Наименовании
                await browser.click(
                    '.src-admin-pages-invoice-components-InvoiceInfo-' +
                        '___invoice-info-module__requisites__close > button'
                );
                await browser.click('.yb-table_payments1c tr:nth-child(1) td:nth-child(8) a');
                await browser.ybAssertView(
                    'таблица Платежи, поп-ап Наименование ФИО',
                    '.yb-invoice-info > section:nth-child(2)',
                    options
                );

                await browser.ybAssertLink('a=Все счета', 'invoices.xml?client_id=43136582');
            });

            it('просмотр таблицы Счета-фактуры на аванс', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=79278002');
                await browser.waitForVisible('.yb-table_oebs-factura');

                await browser.ybAssertView(
                    'таблица Счета-фактуры на аванс',
                    '.yb-invoice-info > section:nth-child(3)'
                );
            });

            it('переплаченный prepayment счет на агентство', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, invoice_id] = await browser.ybRun('test_overpaid_prepayment_with_agency');
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + invoice_id);
                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    'информация о счете, счет на агентство',
                    '.yb-invoice-info',
                    assertViewOpts
                );
            });

            it('пагинация в блоке Платежи', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=79278002');
                await browser.waitForVisible('.yb-table_payments1c');

                await browser.click('.yb-table_payments1c button:nth-child(2)');
                let options = {
                    expandWidth: true
                };
                await browser.ybAssertView(
                    'таблица Платежи, 2 страница',
                    '.yb-invoice-info > section:nth-child(2)',
                    options
                );

                await browser.click(
                    '.yb-table_payments1c ' +
                        '.src-common-components-Table-___table-module__page-size-selector .Button2_theme_clear'
                );
                await browser.ybAssertView(
                    'таблица Платежи, 25 элементов на странице',
                    '.yb-invoice-info > section:nth-child(2)',
                    options
                );
            });

            it('возвраты платежей', async function () {
                const { browser } = this;

                let hideElements = [
                    '.yb-invoice-info .yb-table__doc-date',
                    '.yb-table_payments1c tbody td:first-child'
                ];

                let options = {
                    ignoreElements:
                        '.src-admin-pages-invoice-components-InvoiceInfo-___invoice-info-module__refund-' +
                        'table > div:nth-child(2) > div:nth-child(2)',
                    hideElements,
                    expandWidth: true
                };

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, invoice_id] = await browser.ybRun('test_trust_refund');
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + invoice_id);
                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    'таблица Платежи с доступным возвратом',
                    '.yb-invoice-info > section:nth-child(2)',
                    options
                );

                await browser.click('.yb-table_payments1c button');

                await browser.click('.yb-invoice-refund-modal .Modal-Content .Button2_type_submit');
                await browser.waitForVisible('.yb-table_payments1c tbody td:last-child a');
                await browser.ybAssertView(
                    'таблица Платежи после возврата',
                    '.yb-invoice-info > section:nth-child(2)',
                    options
                );

                hideElements = [
                    '.Modal_visible tbody td:first-child',
                    '.Modal_visible tbody td:nth-child(3)'
                ];
                options = {
                    hideElements
                };
                await browser.click('.yb-table_payments1c tbody td:last-child a');
                await browser.waitForVisible('.Modal_visible .Modal-Content table');
                await browser.ybAssertView(
                    'модальное окно История возвратов',
                    '.Modal_visible .Modal-Content',
                    options
                );
            });
        });
    });
});
