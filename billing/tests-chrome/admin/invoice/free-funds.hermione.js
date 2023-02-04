const { assertViewOpts, hideElements, openConfirm } = require('./helpers');

describe('admin', function () {
    describe('invoice', function () {
        it('зачислить средства с беззаказья, предоплатный счет', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, id] = await browser.ybRun('test_prepayment_underpaid_invoice');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            const assertViewOptsLocal = {
                ignoreElements: ['.yb-invoice-transfer-from-unused-funds button:nth-child(1)']
            };
            await browser.ybAssertView(
                'блок зачисления средств с беззаказья',
                '.yb-invoice-transfer-from-unused-funds',
                assertViewOptsLocal
            );

            await browser.click('#transfer-from-unused-funds__btn');
            await browser.waitForVisible('.yb-invoice-operations tr:nth-child(2)');
            await browser.ybAssertView(
                'предоплатный счет, просмотр после зачисления средств с беззаказья',
                '.yb-content',
                assertViewOpts
            );
        });

        it('исправить беззаказье, предоплатный счет', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, id] = await browser.ybRun('test_prepayment_turned_on_unpaid_invoice');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();
            await browser.ybAssertView('блок исправления беззаказья', '.yb-invoice-correct');

            await openConfirm(browser, 'исправления беззаказья', '#correct__btn');
            await browser.ybAssertView(
                'предоплатный счет, исправление беззаказья',
                '.yb-messages__text'
            );
            await browser.ybMessageAccept();

            await browser.ybWaitForInvisible('#correct__btn');
            await browser.ybWaitForLoad();
            await browser.ybAssertView(
                'предоплатный счет, просмотр после исправления беззаказья',
                '.yb-content',
                assertViewOpts
            );
        });
    });
});
