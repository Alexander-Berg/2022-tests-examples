const { assertViewOpts, openConfirm } = require('./helpers');

describe('admin', function () {
    describe('invoice', function () {
        it('тип prepayment [smoke]', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, , id] = await browser.ybRun('test_prepayment_unpaid_invoice_with_endbuyer');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('страница, prepayment', '.yb-main', assertViewOpts);
        });

        it('тип fictive', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, id] = await browser.ybRun('test_fictive_3_orders');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('страница, fictive', '.yb-main', assertViewOpts);
        });

        it('тип repayment', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, , id] = await browser.ybRun('test_repayment_3_orders');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('страница, repayment', '.yb-main', assertViewOpts);
        });

        it('тип overdraft', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, id] = await browser.ybRun('test_overdraft_overpaid_invoice');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('страница, overdraft', '.yb-main', assertViewOpts);
        });

        it('тип y_invoice', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, , id] = await browser.ybRun('test_fictive_pa_y_invoice');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('страница, y_invoice', '.yb-main', assertViewOpts);
        });

        it('тип personal_account', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=79278002');
            await browser.ybWaitForLoad();

            await browser.ybAssertView('страница, personal_account', '.yb-main', {
                ...assertViewOpts,
                hideElements: [
                    '.yb-page-header__column.yb-page-header__column_right',
                    '.yb-invoice-info .yb-table__manager'
                ]
            });
        });

        it('тип fictive_personal_account', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, , id] = await browser.ybRun('test_fictive_pa_y_invoice');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'страница, fictive_personal_account',
                '.yb-main',
                assertViewOpts
            );
        });

        it('тип els', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=95079537');
            await browser.ybWaitForLoad();

            await browser.ybAssertView('страница, els', '.yb-main', {
                ...assertViewOpts,
                hideElements: ['.src-common-components-Person-___person-module__export-oebs-form']
            });
        });

        it('тип charge-note', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, id] = await browser.ybRun('test_charge_note_and_pa_invoice');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('страница, charge_note', '.yb-main', assertViewOpts);
        });
        it('Проверка ссылок в блоках "Заказы" и "Заявки', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });

            await browser.ybUrl('admin', 'invoice.xml?invoice_id=123461477');
            await browser.ybWaitForLoad();

            await browser.ybAssertLink(
                'a=7-34809894',
                'order.xml?service_cc=PPC&service_order_id=34809894'
            );
            await browser.ybAssertLink('a=Asratyan Petr (41909220)', 'tclient.xml?tcl_id=41909220');

            await browser.ybAssertLink(
                'a=7-34809894',
                'order.xml?service_cc=PPC&service_order_id=34809894'
            );
            await browser.ybAssertLink('a=Asratyan Petr (41909220)', 'tclient.xml?tcl_id=41909220');
        });
        it('Проверка ссылок в основном блоке', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });

            await browser.ybUrl('admin', 'invoice.xml?invoice_id=1211');
            await browser.ybWaitForLoad();

            await browser.ybAssertLink(
                'a=Создать копию',
                'paystep.xml?request_id=1768&person_id=2060&paysys_id=1001&contract_id='
            );
            await browser.ybAssertLink(
                'a=Изменить способ оплаты и плательщика',
                'change_person.xml?invoice_id=1211&person_id=2060&paysys_id=1001&contract_id='
            );
        });

        it('Проверка ссылки на счет на погашение', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });

            await browser.ybUrl('admin', 'invoice.xml?invoice_id=8333979');
            await browser.ybWaitForLoad();

            await browser.ybAssertLink(
                'a=Счет на погашение Б-10339199-1 от 8 февраля 11 г.',
                'invoice.xml?invoice_id=8333990'
            );
        });

        it('Проверка ссылки на акт', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });

            await browser.ybUrl('admin', 'invoice.xml?invoice_id=99244789');
            await browser.ybWaitForLoad();

            await browser.ybAssertLink('a=104136576', 'act.xml?act_id=105258167');
        });
        it('Проверка ссылки на фиктивный счет при переходе из счета на погашение', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });

            await browser.ybUrl('admin', 'invoice.xml?invoice_id=8333990');
            await browser.ybWaitForLoad();

            await browser.ybAssertLink('a=Б-10339190-1', 'invoice.xml?invoice_id=8333979');
        });

        it('просмотр операций', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=99244789');
            await browser.ybWaitForLoad();

            await browser.ybAssertView('просмотр операций', '.yb-invoice-operations');

            await browser.click(
                '.yb-invoice-operations .src-common-components-Table-___table-module__table__pager button'
            );
            await browser.waitForVisible('.yb-invoice-operations tr:nth-child(50)');

            await browser.ybAssertView(
                'просмотр операций, "Загрузить еще"',
                '.yb-invoice-operations'
            );
        });

        it('фильтрация операций по дате', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=99244789');
            await browser.ybWaitForLoad();

            await browser.ybSetSteps('Ищем по дате');
            await browser.ybSetDatepickerValue('.yb-operations-filter__from-dt', '15.07.2020');
            await browser.ybSetDatepickerValue('.yb-operations-filter__to-dt', '20.07.2020');

            await browser.click('.yb-operations-filter__button-show button');
            await browser.ybWaitForInvisible('.yb-invoice-operations tr:nth-child(25)');

            await browser.ybAssertView(
                'просмотр операций, поиск по дате',
                '.yb-invoice-operations'
            );
        });

        it('просмотр актов, пагинация', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=9559028');
            await browser.ybWaitForLoad();

            await browser.ybAssertView('просмотр актов', '.yb-invoice-acts');

            await browser.ybSetSteps('Загрузить еще');
            await browser.click(
                '.yb-invoice-acts .src-common-components-Table-___table-module__table__pager button'
            );
            await browser.waitForVisible('.yb-invoice-acts tr:nth-child(12)');

            await browser.ybAssertView('просмотр актов, "Загрузить еще"', '.yb-invoice-acts');
        });

        it('признание плохим долгом', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, id] = await browser.ybRun('test_overdraft_overdue_invoice');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            let options = {
                hideElements: [
                    '.yb-invoice-acts .yb-table__act-id',
                    '.yb-invoice-acts .yb-table__date',
                    '.yb-invoice-acts tbody td:nth-child(7)'
                ]
            };
            await browser.ybAssertView(
                'просмотр актов, просроченный акт',
                '.yb-invoice-acts',
                options
            );

            await browser.ybSetSteps('Заполняем комментарий и ставим галку');
            await browser.click(
                '.src-admin-pages-invoice-components-Acts-___bad-debts-module__badDebts__comment'
            );
            await browser.setValue(
                '.src-admin-pages-invoice-components-Acts-___bad-debts-module__badDebts__row ' +
                    'textarea',
                'Очень плохой долг'
            );
            await browser.ybSetLcomCheckboxValue(
                '.src-admin-pages-invoice-components-Acts-___bad-debts-module__badDebts__row',
                true
            );
            await browser.ybAssertView(
                'просмотр актов, заполненный комментарий',
                '.yb-invoice-acts',
                options
            );

            await openConfirm(browser, 'признания плохим долгом', '.yb-invoice-acts button');
            await browser.ybAssertView(
                'подтверждение признания плохим долгом',
                '.yb-messages__text'
            );
            await browser.ybMessageAccept();
            await browser.ybWaitForInvisible('.yb-invoice-acts button');
            await browser.ybAssertView(
                'просмотр актов, акт признан плохим долгом',
                '.yb-invoice-acts',
                options
            );
        });
    });
});
