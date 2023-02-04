const { assertViewOpts, hideElements, openConfirm } = require('./helpers');
const { basicIgnore } = require('../../../helpers');

describe('admin', function () {
    describe('invoice', function () {
        describe('rollback button', function () {
            it('возврат неоткрученного в кредит, перенос всей суммы', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, invoice_id, service_order_id] = await browser.ybRun(
                    'test_credit_pa_no_completions'
                );

                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + invoice_id);
                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    'блок снятия с заказа, просмотр',
                    '.yb-invoice-rollback'
                );

                await browser.ybReplaceValue(
                    '.yb-invoice-rollback .yb-suggest-component',
                    '7-' + service_order_id
                );
                await browser.waitForVisible(
                    '.yb-invoice-rollback .yb-suggest-component .yb-suggest__item'
                );
                await browser.click('.yb-invoice-rollback .yb-suggest-component .yb-suggest__item');

                const assertViewOptsLocal = {
                    ignoreElements: ['.yb-invoice-rollback > form > div:nth-child(3) input']
                };
                await browser.ybAssertView(
                    'блок снятия с заказа с выбранным заказом, просмотр',
                    '.yb-invoice-rollback',
                    assertViewOptsLocal
                );

                await openConfirm(browser, 'rollback', '#rollback__btn');

                await browser.ybAssertView(
                    'блок снятия с заказа, все средства, подтверждение',
                    '.yb-messages__text'
                );

                await browser.ybMessageAccept();

                await browser.ybWaitForInvisible('#rollback__btn');
                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    'постоплатный счет, просмотр после переноса всего неоткрученного в кредит',
                    '.yb-main',
                    assertViewOpts
                );
            });

            it('возврат неоткрученного в кредит, ошибки', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, invoice_id, service_order_id] = await browser.ybRun(
                    'test_credit_pa_no_completions'
                );

                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + invoice_id);
                await browser.ybWaitForLoad();

                await browser.ybReplaceValue(
                    '.yb-invoice-rollback > form > div:nth-child(1) input',
                    '0'
                );
                await browser.ybAssertView(
                    'блок снятия с заказа, ошибка - введен 0',
                    '.yb-invoice-rollback'
                );

                await browser.ybReplaceValue(
                    '.yb-invoice-rollback > form > div:nth-child(1) input',
                    '10000000'
                );
                await browser.ybAssertView(
                    'блок снятия с заказа, ошибка - слишком большая сумма',
                    '.yb-invoice-rollback'
                );

                await browser.ybReplaceValue(
                    '.yb-invoice-rollback > form > div:nth-child(1) input',
                    '1.123123'
                );
                await browser.ybAssertView(
                    'блок снятия с заказа, ошибка - много знаков после запятой',
                    '.yb-invoice-rollback'
                );
            });

            it('возврат неоткрученного в кредит, перенос части суммы', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, invoice_id, service_order_id] = await browser.ybRun(
                    'test_credit_pa_no_completions'
                );

                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + invoice_id);
                await browser.ybWaitForLoad();

                await browser.ybReplaceValue(
                    '.yb-invoice-rollback > form > div:nth-child(1) input',
                    '100'
                );
                await browser.ybLcomSelect('.yb-invoice-rollback', 'Возврат');
                await browser.ybReplaceValue(
                    '.yb-invoice-rollback .yb-suggest-component',
                    '7-' + service_order_id
                );
                await browser.waitForVisible(
                    '.yb-invoice-rollback .yb-suggest-component .yb-suggest__item'
                );
                await browser.click('.yb-invoice-rollback .yb-suggest-component .yb-suggest__item');

                const assertViewOptsLocal = {
                    ignoreElements: ['.yb-invoice-rollback > form > div:nth-child(3) input']
                };
                await browser.ybAssertView(
                    'блок снятия с заказа с выбранным заказом, часть суммы, просмотр',
                    '.yb-invoice-rollback',
                    assertViewOptsLocal
                );
                await browser.click('#rollback__btn');

                await browser.waitForVisible('.yb-invoice-operations tr:nth-child(5)');

                await browser.ybAssertView(
                    'постоплатный счет, просмотр после переноса части неоткрученного в кредит',
                    '.yb-main',
                    assertViewOpts
                );
            });

            it('возврат средств, предоплатный счет, перенос части суммы', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, invoice_id] = await browser.ybRun('test_prepayment_overpaid_invoice');

                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + invoice_id);
                await browser.ybWaitForLoad();

                await browser.ybReplaceValue(
                    '.yb-invoice-rollback > form > div:nth-child(1) input',
                    '100.23'
                );
                await browser.ybLcomSelect('.yb-invoice-rollback', 'Возврат');

                await browser.ybAssertView(
                    'блок снятия с заказа, предоплатный, часть суммы, просмотр',
                    '.yb-invoice-rollback'
                );
                await browser.click('#rollback__btn');

                await browser.waitForVisible('.yb-invoice-operations tr:nth-child(5)');

                assertViewOptsLocal = {
                    ignoreElements: [
                        ...basicIgnore,
                        '.yb-invoice-patch-date input',
                        '.yb-transfer-orders__order button',
                        '.yb-invoice-transfer-from-unused-funds button:nth-child(1)'
                    ],
                    hideElements,
                    captureElementFromTop: true,
                    allowViewportOverflow: true,
                    compositeImage: true,
                    expandWidth: true
                };

                await browser.ybAssertView(
                    'предоплатный счет, просмотр после переноса части средств',
                    '.yb-main',
                    assertViewOptsLocal
                );
            });
        });
    });
});
