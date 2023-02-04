const { hideElements, setDate } = require('./helpers');

describe('user', () => {
    describe('invoices', () => {
        it('нет счетов', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            await browser.ybRun('create_client_for_user', { login });

            await browser.ybUrl('user', `invoices.xml`);
            await browser.waitForVisible('.content');
            await browser.ybAssertView('просмотр страницы счетов, нет счетов', '.content');
        });

        it('поиск по дате и ссылки [smoke]', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const [
                ,
                ,
                ,
                invoice_id,
                external_id
            ] = await browser.ybRun('test_prepayment_fixed_dt_invoices', { login });

            await browser.ybUrl('user', `invoices.xml`);
            await browser.waitForVisible('.content');
            await setDate(browser, '.xf-input');
            await browser.click('input[type="submit"]');
            await browser.ybAssertView('поиск по дате', '.sub:nth-child(6)', { hideElements });

            await browser.ybAssertLink(`a=${external_id}`, `invoice.xml?invoice_id=${invoice_id}`);
        });

        it('просроченные овердрафты', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const [
                ,
                invoice_id,
                external_id
            ] = await browser.ybRun('test_overdraft_almost_overdue_and_overdue_invoice', { login });

            await browser.ybUrl('user', `invoices.xml`);
            await browser.waitForVisible('.content');

            const localHideElements = [
                ...hideElements,
                'td.b-payments__td:nth-child(2)',
                '.sub-border td:nth-child(1)',
                '.sub-border td:nth-child(2)'
            ];
            await browser.ybAssertView(
                'просмотр страницы счетов, просроченные овердрафты',
                '.content',
                { hideElements: localHideElements }
            );

            await browser.ybAssertLink(`a=${external_id}`, `invoice.xml?invoice_id=${invoice_id}`);
        });

        it('фильтрация по сервису и способу оплаты', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({ login: 'yb-hermione-ci-1' });

            await browser.ybUrl('user', `invoices.xml`);
            await browser.waitForVisible('.content');
            await browser.ybAssertView('просмотр страницы счетов, есть счета', '.content', {
                hideElements
            });

            await browser.click('.xf-advancer .xf-label');
            await browser.selectByValue('select[name="service_group_id"]', '7');
            await browser.selectByValue('select[name="paysys_cc"]', 'ur');
            await browser.ybAssertView(
                'просмотр заполненного расширенного фильтра, сервис и способ оплаты',
                '.xf-form'
            );

            await browser.click('input[type="submit"]');
            await browser.waitForVisible('.content');
            await browser.ybAssertView(
                'просмотр страницы счетов, результат поиска',
                '.sub:nth-child(6)',
                { hideElements }
            );
        });

        it('пагинация, итого', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({ login: 'yb-hermione-ci-1' });

            await browser.ybUrl('user', `invoices.xml`);
            await browser.waitForVisible('.content');
            await browser.click('.pages a');
            await browser.waitForVisible('.content');
            await browser.ybAssertView('просмотр страницы счетов, вторая страница', '.content', {
                hideElements
            });

            await browser.click('.xf-advancer .xf-label');
            await browser.selectByValue('select[name="ps"]', '20');
            await browser.click('input[name="ct"]');
            await browser.ybAssertView(
                'просмотр заполненного расширенного фильтра, количество строк и итоги',
                '.xf-form'
            );

            await browser.click('input[type="submit"]');
            await browser.waitForVisible('.content');
            await browser.ybAssertView(
                'просмотр страницы счетов, 20 строк и итоги',
                '.sub:nth-child(6)',
                { hideElements }
            );
        });
        it('счета, предупреждения о входе под логином без клиента', async function () {
            const { browser } = this;
            await browser.ybSignIn({ login: 'yb-static-balance-5' });
            await browser.ybUrl('user', `invoices.xml`);
            await browser.waitForVisible('.yb-notification_type_error');
        });
    });
});
