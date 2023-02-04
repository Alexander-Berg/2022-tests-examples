const { setClient } = require('./helpers');
const assert = require('chai').assert;
const { Roles } = require('../../../helpers/role_perm');

describe('admin', function () {
    describe('corrections', function () {
        it('создание корректировки', async function () {
            const { browser } = this;

            await browser.ybSignIn({ baseRole: Roles.Support, include: [], exclude: [] });

            await browser.ybUrl('admin', 'corrections.xml');
            await browser.ybWaitForLoad();

            await browser.ybAssertView('просмотр пустой страницы корректировок', '.yb-content');

            await browser.ybSetSteps(`Выбирает шаблон`);
            await browser.ybLcomSelect(
                '.src-common-presentational-components-Field-___field-module__detail__value',
                'Гермионовый шаблон'
            );
            await browser.ybAssertView('просмотр незаполненного шаблона', '.yb-content');

            await browser.ybSetSteps(`Заполняет шаблон`);
            await setClient(browser, 7943961);
            await browser.waitForVisible('div[data-detail-id="contract_id"] .Select2');
            await browser.ybLcomSelect('div[data-detail-id="contract_id"]', '92390/18 (364284)');
            await browser.waitForVisible('div[data-detail-id="invoice_eid"] .Select2');
            await browser.ybLcomSelect(
                'div[data-detail-id="invoice_eid"]',
                'ЛСТ-860798663-1 (AGENT_REWARD)'
            );
            await browser.ybLcomSelect('div[data-detail-id="service_id"]', 'Яндекс.Такси: Платежи');
            await browser.ybLcomSelect('div[data-detail-id="transaction_type"]', 'Платёж');
            await browser.ybReplaceValue('div[data-detail-id="payment_id"]', '666666');
            await browser.ybReplaceValue('div[data-detail-id="trust_payment_id"]', '666666666');
            await browser.ybSetLcomCheckboxValue('div[data-detail-id="auto"]', true);
            await browser.ybSetLcomCheckboxValue('div[data-detail-id="internal"]', true);
            await browser.ybReplaceValue('div[data-detail-id="product_id"]', '507264');
            await browser.ybReplaceValue('div[data-detail-id="amount"]', '123.12');
            await browser.ybReplaceValue('div[data-detail-id="amount_fee"]', '23.12');
            await browser.ybReplaceValue('div[data-detail-id="yandex_reward"]', '1.45');
            await browser.ybReplaceValue('div[data-detail-id="yandex_reward_wo_nds"]', '1.2');
            await browser.ybReplaceValue('div[data-detail-id="dt"]', '01.01.2021');
            await browser.ybClickOut();
            await browser.ybReplaceValue('div[data-detail-id="payout_ready_dt"]', '01.02.2021');
            await browser.ybClickOut();
            await browser.ybReplaceValue('div[data-detail-id="invoice_id"]', '78784135');
            await browser.ybReplaceValue('div[data-detail-id="client_id"]', '7943961');
            await browser.ybReplaceValue('div[data-detail-id="comments"]', 'вжух');
            await browser.ybAssertView('просмотр заполненного шаблона', '.yb-content');

            await browser.ybSetSteps(`Отправляет корректировку`);
            await browser.click('#corr-submit');
            await browser.waitForVisible('.yb-corrections__success');
            let successMessage = await browser.getText('.yb-corrections__success');
            assert.equal(
                'Корректировка  успешно создана',
                successMessage.replace(/[\d]/g, ''),
                'Сообщения не совпадают'
            );
        });
    });
});
