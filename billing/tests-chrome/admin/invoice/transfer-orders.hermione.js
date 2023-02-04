const { assertViewOpts, openConfirm } = require('./helpers');

describe('admin', function () {
    describe('invoice', function () {
        describe('перенос средств на заказ', function () {
            it('положить все средства на заказ со скидкой', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, id] = await browser.ybRun('test_prepayment_overpaid_invoice');
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
                await browser.ybWaitForLoad();

                let options = {
                    ignoreElements: ['.yb-transfer-orders__order button']
                };
                await browser.ybAssertView(
                    'блок переноса на заказ',
                    '.yb-invoice-transfer-orders',
                    options
                );

                await browser.ybReplaceValue('input#transfer-orders__discount', '10');
                await openConfirm(browser, 'переноса средств', '#transfer-orders__transfer-btn');
                await browser.ybAssertView(
                    'подтверждение переноса всех средств',
                    '.yb-messages__text'
                );
                await browser.ybMessageAccept();
                await browser.ybWaitForInvisible('#transfer-orders__transfer-btn');

                await browser.ybAssertView(
                    'просмотр страницы после переноса всех средств',
                    '.yb-content',
                    assertViewOpts
                );
            });

            it('положить часть средств на произвольный заказ [smoke]', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, id] = await browser.ybRun('test_prepayment_overpaid_invoice');
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
                await browser.ybWaitForLoad();

                await browser.ybSetSteps('Заполняет поля для перевода');
                await browser.ybReplaceValue('.yb-transfer-orders__arbitrary-order', '7-45046484');
                await browser.click('label.transfer-orders__src-pts-radio');
                await browser.ybReplaceValue(
                    '.yb-invoice-transfer-orders div:nth-child(4) div:nth-child(2) input',
                    '123.12'
                );
                await browser.ybLcomSelect(
                    '.src-admin-pages-invoice-components-TransferOrders-___style-module__unused-funds-value__select',
                    'Взаимозачет'
                );

                await browser.ybAssertView(
                    'заполненный блок переноса средств на заказ',
                    '.yb-invoice-transfer-orders'
                );

                await openConfirm(browser, 'переноса средств', '#transfer-orders__transfer-btn');
                await browser.ybMessageAccept();
                await browser.ybWaitForInvisible('.yb-transfer-orders__button-transfer_progress');

                await browser.ybAssertView(
                    'блок переноса средств на заказ после переноса',
                    '.yb-invoice-transfer-orders'
                );
            });

            it('проверка ошибок', async function () {
                const { browser } = this;

                await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                const [, id] = await browser.ybRun('test_prepayment_overpaid_invoice');
                await browser.ybUrl('admin', 'invoice.xml?invoice_id=' + id);
                await browser.ybWaitForLoad();

                await browser.ybSetSteps('Слишком большая сумма и скидка');
                await browser.click('label.transfer-orders__src-pts-radio');
                await browser.ybReplaceValue(
                    '.yb-invoice-transfer-orders div:nth-child(4) div:nth-child(2) input',
                    '12312'
                );
                await browser.ybReplaceValue('input#transfer-orders__discount', '1000');

                let options = {
                    ignoreElements: ['.yb-transfer-orders__order button']
                };
                await browser.ybAssertView(
                    'блок переноса на заказ, сумма больше максимальной, некорректная скидка',
                    '.yb-invoice-transfer-orders',
                    options
                );
            });
        });
    });
});
