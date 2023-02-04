const {
    hideElements,
    assertViewOpts,
    openOrder,
    transferSetArbitraryOrder,
    transferSetOrder,
    transferSetAmount,
    transferSetDiscount,
    transferSumbit,
    transferWait,
    assertNotTransferable,
    operationsWaitLoad,
    operationsLoadMore,
    operationsShowByDate,
    paySetType,
    paySetAmount,
    payClick,
    payConfirm,
    payAbort,
    payWaitLoad
} = require('./helpers');
const { basicIgnore } = require('../../../helpers');

describe('admin', function () {
    describe('order', function () {
        it('ссылки в информации о заказе, пустой заказ на агентство с примечанием', async function () {
            const { browser } = this;

            const [clientId, , serviceOrderId, orderId, agencyId] = await openOrder(
                browser,
                'test_agency_no_request_order'
            );

            await browser.ybAssertLink(
                'a=Все заказы клиента',
                'orders.xml?client_id=' + String(clientId) + '&pn=1&ct=1'
            );
            await browser.ybAssertLink(
                'a=Все заказы агентства',
                'orders.xml?agency_id=' + String(agencyId) + '&pn=1&ct=1'
            );
            await browser.ybAssertLink(
                'a=Все счета агентства',
                'invoices.xml?client_id=' + String(agencyId) + '&pn=1&ct=1'
            );
            await browser.ybAssertLink(
                'a=Нотифицировать проект',
                'notify-order.xml?order_id=' +
                    String(orderId) +
                    '&retpath=%2Forder.xml%3Fservice_cc%3DPPC%26service_order_id%3D' +
                    String(serviceOrderId)
            );
            await browser.ybAssertView(
                'страница, заказ на агентство с примечанием',
                '.yb-content',
                assertViewOpts
            );
        });

        it('перевод средств на несуществующий и другой заказ', async function () {
            const { browser } = this;

            const [, , , , invoiceId] = await openOrder(
                browser,
                'test_client_consume_compl_and_empty_order'
            );

            await browser.ybAssertLink(
                '.yb-consumes-list__invoice-id a',
                `invoice.xml?invoice_id=${invoiceId}`
            );

            await transferSetArbitraryOrder(browser, '1234-12341234');
            await transferSumbit(browser);
            await transferWait(browser);
            await browser.ybAssertView(
                'перевод средств, перевод средств на несуществующий заказ',
                '.yb-transfer-orders',
                assertViewOpts
            );

            await transferSetOrder(browser, '7');
            await transferSumbit(browser);
            await transferWait(browser);
            await browser.ybAssertView(
                'страница, перевод средств на другой заказ',
                '.yb-content',
                assertViewOpts
            );
        });

        it('перевод средств на беззаказье', async function () {
            const { browser } = this;

            await openOrder(browser, 'test_client_fix_discount');

            await transferSetAmount(browser, '1000');
            await browser.ybAssertView(
                'перевод средств, 1000 у.е. на беззаказье',
                '.yb-transfer-orders',
                assertViewOpts
            );

            await transferSetAmount(browser, '10.123123');
            await transferSumbit(browser);
            await transferWait(browser);
            await browser.ybAssertView(
                'страница, перевод 10.123123 у.е. на беззаказье',
                '.yb-content',
                assertViewOpts
            );
        });

        it('перевод дробного количества на произвольный заказ', async function () {
            const { browser } = this;

            await openOrder(browser, 'test_client_consume_compl_and_empty_order');

            await transferSetOrder(browser, '7');
            await transferSetAmount(browser, '123.1');
            await transferSetDiscount(browser, '200');
            await browser.ybAssertView('перевод средств, со скидкой 200%', '.yb-transfer-orders', {
                ignoreElements: basicIgnore,
                hideElements: [...hideElements, '.yb-transfer-orders__order']
            });

            await transferSetDiscount(browser, '10');
            await transferSumbit(browser);
            await transferWait(browser);
            await browser.ybAssertView(
                'страница, перевод 123.1 у.е. со скидкой 10% на другой заказ',
                '.yb-content',
                {
                    ignoreElements: basicIgnore,
                    hideElements: [...hideElements, '.yb-transfer-orders__order']
                }
            );
        });

        it('перевод всех средств на беззаказье без скидки', async function () {
            const { browser } = this;

            await openOrder(browser, 'test_client_consume_compl_and_empty_order');

            await transferSetOrder(browser, 'Беззаказье');
            await browser.ybAssertView(
                'перевод всех средств на беззаказье',
                '.yb-transfer-orders',
                {
                    ignoreElements: basicIgnore,
                    hideElements
                }
            );

            await transferSumbit(browser);
            await transferWait(browser);
            await browser.ybAssertView(
                'страница, перевод всех средств на беззаказье без скидки',
                '.yb-content',
                {
                    ignoreElements: basicIgnore,
                    hideElements
                }
            );
        });

        it('пагинация, сортировка и количество элементов в заявках', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'order.xml?order_id=6984457');
            await browser.ybWaitForLoad();

            await browser.ybAssertLink('a=Б-56956778-1', 'invoice.xml?invoice_id=42380976');

            await browser.ybTableChangeSort('Дата заявки');

            await browser.ybTableChangePageNumber(2);
            await browser.ybWaitForInvisible(
                '.src-common-components-Table-___table-module__table_updating'
            );
            await browser.ybAssertView(
                'страница, переключение заявок на 2 страницу',
                '.yb-content',
                {
                    ignoreElements: basicIgnore
                }
            );

            await browser.ybTableChangePageSize(25);
            await browser.ybWaitForInvisible(
                '.src-common-components-Table-___table-module__table_updating'
            );
            await browser.ybAssertView(
                'страница, переключение заявок на отображение по 25 страниц',
                '.yb-content',
                {
                    ignoreElements: basicIgnore
                }
            );
        });

        it('перенос между сервисами с овердрафта невозможен', async function () {
            const { browser } = this;

            const [clientId, invoiceId, externalId, orderId] = await browser.ybRun(
                'test_overdraft_overpaid_invoice'
            );
            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl(
                'admin',
                'order.xml?service_cc=PPC&service_order_id=' + String(orderId)
            );
            await browser.ybWaitForLoad();

            await transferSetArbitraryOrder(browser, '11-21615325');
            await transferSumbit(browser);
            await assertNotTransferable(browser);
        });

        it('пагинация, сортировка, количество элементов и проверка ссылки в счетах', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'order.xml?service_cc=PPC&service_order_id=7750101');
            await browser.ybWaitForLoad();

            await browser.ybAssertLink('a=R-39583969-1', 'invoice.xml?invoice_id=33222357');
            await browser.ybAssertLink('a=R-52320793-1', 'invoice.xml?invoice_id=39662881');
            await browser.ybAssertLink('a=R-36685025-1', 'invoice.xml?invoice_id=30805705');

            {
                await browser.ybTableChangeSort(
                    'Дата счета',
                    '.yb-invoice-orders_without-receipts'
                );
                await browser.ybTableChangePageNumber(2, '.yb-invoice-orders_without-receipts');
                await browser.ybAssertView(
                    'таблица неоплаченных счетов, переключение на 2 страницу',
                    '.yb-invoice-orders_without-receipts',
                    {
                        ignoreElements: basicIgnore
                    }
                );
                await browser.ybTableChangePageSize(25, '.yb-invoice-orders_without-receipts');
                await browser.ybAssertView(
                    'таблица неоплаченных счетов, переключение на отображение по 25 страниц',
                    '.yb-invoice-orders_without-receipts',
                    {
                        ignoreElements: basicIgnore
                    }
                );
            }

            {
                await browser.ybTableChangeSort('Дата счета', '.yb-invoice-orders_with-receipts');
                await browser.ybTableChangePageNumber(2, '.yb-invoice-orders_with-receipts');
                await browser.ybAssertView(
                    'таблица оплаченных счетов, переключение на 2 страницу',
                    '.yb-invoice-orders_with-receipts',
                    {
                        ignoreElements: basicIgnore
                    }
                );
                await browser.ybTableChangePageSize(25, '.yb-invoice-orders_with-receipts');
                await browser.ybAssertView(
                    'таблица оплаченных счетов, переключение на отображение по 25 страниц',
                    '.yb-invoice-orders_with-receipts',
                    {
                        ignoreElements: basicIgnore
                    }
                );
            }

            {
                await browser.ybTableChangeSort('Дата счета', '.yb-invoice-orders_credit');
                await browser.ybTableChangePageNumber(2, '.yb-invoice-orders_credit');
                await browser.ybAssertView(
                    'таблица кредитных счетов, переключение на 2 страницу',
                    '.yb-invoice-orders_credit',
                    {
                        ignoreElements: basicIgnore
                    }
                );
                await browser.ybTableChangePageSize(25, '.yb-invoice-orders_credit');
                await browser.ybAssertView(
                    'таблица кредитных счетов, переключение на отображение по 25 страниц',
                    '.yb-invoice-orders_credit',
                    {
                        ignoreElements: basicIgnore
                    }
                );
            }
        });

        it('пагинация, сортировка, количество элементов и проверка ссылки в недовыставленных счетах', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'order.xml?order_id=6931735');
            await browser.ybWaitForLoad();

            await browser.ybAssertLink('a=Б-48024827', 'paystep.xml?request_id=48024827');

            await browser.ybTableChangeSort('Дата счета', '.yb-order-requests');
            await browser.ybTableChangePageNumber(2, '.yb-order-requests');
            await browser.ybWaitForInvisible('.yb-order-requests_updating');

            await browser.ybAssertView(
                'таблица недовыставленных счетов, переключение на 2 страницу',
                '.yb-order-requests',
                {
                    ignoreElements: basicIgnore
                }
            );

            await browser.ybTableChangePageSize(25, '.yb-order-requests');
            await browser.ybWaitForInvisible('.yb-order-requests_updating');

            await browser.ybAssertView(
                'таблица недовыставленных счетов, переключение на отображение по 25 страниц',
                '.yb-order-requests',
                {
                    ignoreElements: basicIgnore
                }
            );
        });

        it('загрузить ещё, поиск по дате, сортировки и проверка ссылки в операциях', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('admin', 'order.xml?service_cc=PPC&service_order_id=7750101');
            await browser.ybWaitForLoad();

            await browser.ybTableChangeSort('Дата', '.yb-operations');
            await operationsWaitLoad(browser);
            await operationsLoadMore(browser);
            await browser.ybAssertView('операции, сортировка и загрузить ещё', '.yb-operations', {
                ignoreElements: basicIgnore
            });

            await browser.ybAssertLink('a=R-30163892-1', 'invoice.xml?invoice_id=25146557');
            await operationsShowByDate(browser);
            await browser.ybAssertView('операции, фильтрация по дате', '.yb-operations', {
                ignoreElements: basicIgnore
            });
        });

        it('оплата компенсацией, значение по умолчанию', async function () {
            const { browser } = this;

            await openOrder(browser, 'test_agency_no_request_order');

            await payClick(browser);
            await payConfirm(browser);
            await payWaitLoad(browser);

            await browser.ybAssertView(
                'страница, оплата компенсацией',
                '.yb-content',
                assertViewOpts
            );
        });

        it('оплата сертификатом, дробное', async function () {
            const { browser } = this;

            await openOrder(browser, 'test_agency_no_request_order');

            await paySetType(browser, 'Сертификат');
            await paySetAmount(browser, '12,34');
            await payClick(browser);
            await payConfirm(browser);
            await payWaitLoad(browser);

            await browser.ybAssertView(
                'страница, оплата сертификатом',
                '.yb-content',
                assertViewOpts
            );
        });

        it('отмена оплаты', async function () {
            const { browser } = this;

            await openOrder(browser, 'test_agency_no_request_order');

            await payClick(browser);
            await payAbort(browser);
            await payWaitLoad(browser);

            await browser.ybAssertView('страница, отмена оплаты', '.yb-content', assertViewOpts);
        });

        it('без прав CreateCerificatePayments и TransferBetweenClients', async function () {
            const { browser } = this;

            await openOrder(browser, 'test_client_consume_compl_and_empty_order', {
                isReadonly: true
            });
            await browser.ybAssertView('страница, без блоков оплаты и переноса', '.yb-content', {
                ignoreElements: basicIgnore,
                hideElements: [...hideElements, '.yb-operations']
            });
        });
    });
});
