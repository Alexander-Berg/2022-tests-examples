const { assertViewOpts, openConfirm } = require('./helpers');
const { basicIgnore } = require('../../../helpers');

describe('admin', function () {
    describe('invoice', function () {
        it('перенос даты счета', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, id] = await browser.ybRun('test_prepayment_unpaid_invoice');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            let options = {
                ignoreElements: ['.yb-invoice-patch-date .datepicker-input']
            };
            await browser.ybAssertView(
                'блок изменения даты счета',
                '.yb-invoice-patch-date',
                options
            );

            await browser.click('.yb-invoice-patch-date .datepicker-input');
            await browser.click('.react-datepicker__day--001.react-datepicker__day--outside-month');

            await browser.ybAssertView(
                'блок изменения даты счета после выбора новой даты',
                '.yb-invoice-patch-date',
                options
            );

            await browser.click('.yb-invoice-patch-date .Button2');
        });

        it('изменение суммы счета', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, id] = await browser.ybRun('test_prepayment_unpaid_invoice');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            await browser.ybAssertView('блок изменения суммы счета', '.yb-invoice-patch-sum');

            await browser.ybReplaceValue('.yb-invoice-patch-sum input', '1000');

            await browser.ybAssertView(
                'блок изменения суммы счета после ввода новой суммы',
                '.yb-invoice-patch-sum'
            );

            await browser.click('.yb-invoice-patch-sum .Button2');
            await browser.ybMessageAccept();

            await browser.ybWaitForInvisible('#confirm-payment__btn');
            await browser.ybWaitForLoad();
            await browser.ybAssertView(
                'блок изменения суммы счета после изменения суммы',
                '.yb-invoice-patch-sum'
            );
        });

        it('изменение договора счета', async function () {
            const { browser } = this;

            await browser.ybSignIn({ isAdmin: true, isReadonly: false });
            const [, , id] = await browser.ybRun('test_prepayment_unpaid_invoice_with_endbuyer');
            await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
            await browser.ybWaitForLoad();

            let options = {
                ignoreElements: ['.yb-invoice-contract button:nth-child(1)']
            };
            await browser.ybAssertView(
                'блок изменения договора счета',
                '.yb-invoice-contract',
                options
            );

            await browser.ybSetLcomSelectValue(
                '.yb-invoice-contract button:nth-child(1)',
                'Отвязать'
            );

            await browser.ybAssertView(
                'блок изменения договора счета после выбора "Отвязать"',
                '.yb-invoice-contract'
            );

            await openConfirm(browser, 'изменения договора', '#contract__btn');

            await browser.ybAssertView('подтверждение изменения договора', '.yb-messages__text');

            await browser.ybMessageAccept();

            await browser.ybWaitForInvisible('#contract__btn');
            await browser.ybWaitForLoad();
            await browser.ybAssertView(
                'блок изменения договора счета после отвязки договора',
                '.yb-invoice-contract'
            );
        });
    });
});
