const { assertViewOpts, openConfirm, waitForPageReload } = require('./helpers');

describe('admin', function () {
    describe('invoice', function () {
        it('проверка оплаты, предоплатный счет, зеленая подсветка', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, id] = await browser.ybRun('test_prepayment_unpaid_invoice');

            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('предоплатный счет, просмотр', '.yb-main', assertViewOpts);

            await openConfirm(browser, 'внесения оплаты', '#confirm-payment__btn');

            await browser.ybAssertView('предоплатный счет, подтверждение', '.yb-messages__text');

            await browser.ybMessageAccept();

            await browser.ybWaitForInvisible('#confirm-payment__btn');
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'предоплатный счет, просмотр после оплаты',
                '.yb-main',
                assertViewOpts
            );
        });

        it('проверка оплаты, овердрафтный счет, без подсветки', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [id] = await browser.ybRun(
                'test_prepayment_paid_invoice_with_act_and_overdraft_unpaid'
            );

            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('овердрафтный счет, просмотр', '.yb-main', assertViewOpts);

            await openConfirm(browser, 'внесения оплаты', '#confirm-payment__btn');

            await browser.ybAssertView('овердрафтный счет, подтверждение', '.yb-messages__text');

            await browser.ybMessageAccept();

            await browser.ybWaitForInvisible('#confirm-payment__btn');
            await browser.ybWaitForLoad();

            await browser.ybAssertView(
                'овердрафтный счет, просмотр после оплаты',
                '.yb-main',
                assertViewOpts
            );
        });
    });
});
