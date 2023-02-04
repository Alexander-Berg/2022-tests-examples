const { assertViewOpts } = require('./helpers');
const { waitUntilTimeout } = require('../../../helpers');

describe('user', () => {
    describe('invoice', () => {
        it('просмотр пустого prepayment счета под клиентом', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const [, id] = await browser.ybRun('test_prepayment_unpaid_invoice', { login });
            await browser.ybUrl('user', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

            await browser.ybAssertView(
                'страница, prepayment пустой под клиентом',
                '.yb-user-content',
                assertViewOpts
            );
        });

        it('просмотр prepayment счета под админом', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('user', 'invoice.xml?invoice_id=129806710');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

            await browser.ybAssertView('страница, prepayment под админом', '.yb-user-content');

            await browser.click('.b-link_inner');
            await browser.ybAssertView(
                'страница, prepayment под админом - все атрибуты плательщика',
                '.yb-user-content'
            );

            await browser.ybAssertLink(
                'a=7-57660567',
                'order.xml?service_cc=PPC&service_order_id=57660567'
            );
        });

        it('просмотр просроченного overdraft счета под клиентом', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const [, id] = await browser.ybRun('test_overdraft_overdue_invoice', { login });
            await browser.ybUrl('user', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

            await browser.ybAssertView(
                'страница, просроченный overdraft под клиенто',
                '.yb-user-content',
                assertViewOpts
            );
        });

        it('перенос свободных средств под клиентом', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const [, id] = await browser.ybRun('test_prepayment_overpaid_invoice', { login });
            await browser.ybUrl('user', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

            await browser.ybAssertView(
                'страница, переплаченный prepayment со свободными средствами',
                '.yb-user-content',
                assertViewOpts
            );

            await browser.click('input[value="Зачислить"]');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
            await browser.ybAssertView(
                'страница, переплаченный prepayment после переноса средств',
                '.yb-user-content',
                assertViewOpts
            );
        });

        it('просмотр personal_account, пагинация актов под админом', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('user', 'invoice.xml?invoice_id=79278002');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

            await browser.ybAssertView(
                'страница, personal_account под админом',
                '.yb-user-content'
            );

            await browser.click('#invoice_acts_data_a_next');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
            await browser.ybAssertView('пагинация актов', '#invoice_acts_data');
        });

        it('просмотр repayment под админом', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('user', 'invoice.xml?invoice_id=8333990');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

            await browser.ybAssertView('страница, repayment под админом', '.yb-user-content');
        });

        it('просмотр fictive_personal_account, пагинация в зачислениях под админом', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('user', 'invoice.xml?invoice_id=73377265');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

            await browser.ybAssertView(
                'страница, fictive_personal_account под админом',
                '.yb-user-content'
            );

            await browser.click('#invoice_consumes-list-container_a2');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
            await browser.ybAssertView('пагинация зачислений', '#invoice_consumes-list-container');
        });

        it('просмотр fictive_personal_account, пагинация в операциях под админом', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true });
            await browser.ybUrl('user', 'invoice.xml?invoice_id=73377265');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

            await browser.click('.content div.sub:nth-last-child(1) > div a');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);
            await browser.ybAssertView('пагинация операций', '.content div.sub:nth-last-child(1)');
        });

        it('просмотр пустого prepayment счета под бухлогином', async function () {
            const { browser } = this;

            const { login } = await browser.ybSignIn({});
            const [client_id, id] = await browser.ybRun('test_prepayment_unpaid_invoice');
            await browser.ybRun('test_delete_every_accountant_role', { login });
            await browser.ybRun('test_unlink_client', { login });
            await browser.ybRun('test_add_accountant_role', { login, client_id });
            await browser.ybUrl('user', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]', waitUntilTimeout);

            await browser.ybAssertView(
                'страница, prepayment пустой под бухлогином',
                '.yb-user-content',
                assertViewOpts
            );
            await browser.ybRun('test_delete_every_accountant_role', { login });
        });
    });
});
